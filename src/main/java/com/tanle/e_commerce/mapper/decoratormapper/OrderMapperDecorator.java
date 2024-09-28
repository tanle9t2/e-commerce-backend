package com.tanle.e_commerce.mapper.decoratormapper;

import com.tanle.e_commerce.Repository.Jpa.TenantRepository;
import com.tanle.e_commerce.Repository.Jpa.UserRepository;
import com.tanle.e_commerce.dto.OrderDTO;
import com.tanle.e_commerce.entities.*;
import com.tanle.e_commerce.exception.ResourceNotFoundExeption;
import com.tanle.e_commerce.mapper.OrderDetailMapper;
import com.tanle.e_commerce.mapper.OrderMapper;
import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

@Mapper
public abstract class OrderMapperDecorator implements OrderMapper {
    @Autowired
    private OrderMapper delegate;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private UserRepository userRepository;

    @Override
    public OrderDTO convertDTO(Order order) {
        OrderDTO orderDTO = delegate.convertDTO(order);

        orderDTO.setUserOrder(orderDTO.buildUserOrder(order.getUser().getId(),
                order.getUser().getFirstName()+ " " + order.getUser().getLastName()));
        if (order.getOrderDetails() != null) {
            double totalPrice = order.getOrderDetails().stream()
                    .mapToDouble(o -> o.getSku().getPrice() * o.getQuantity())
                    .sum();
            int quantity = order.getOrderDetails().size();
            orderDTO.setQuantity(quantity);
            orderDTO.setTotalPrice(totalPrice);
        }
        return orderDTO;
    }

    @Override
    public Order convertEntity(OrderDTO orderDTO) {
        Tenant tenant = tenantRepository.findById(orderDTO.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found tenant"));
        User user = userRepository.findById(orderDTO.getUserOrder().getUserId())
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found user"));
        Address address = user.getAddresses().stream()
                .filter(a -> a.getId() == orderDTO.getReceiptAddress().getId())
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found address"));
        Order order = delegate.convertEntity(orderDTO);
        List<OrderDetail> orderDetails = orderDTO.getItemList().stream()
                .map(o -> orderDetailMapper.convertEntity(o))
                .collect(Collectors.toList());;
        order.addOrderDetail(orderDetails);
        order.setAddress(address);
        order.setTenant(tenant);
        order.setUser(user);
        return order;
    }
}
