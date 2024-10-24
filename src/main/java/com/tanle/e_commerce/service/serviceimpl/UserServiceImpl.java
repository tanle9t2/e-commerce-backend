package com.tanle.e_commerce.service.serviceimpl;


import com.tanle.e_commerce.Repository.Jpa.AddressRepository;
import com.tanle.e_commerce.Repository.Jpa.RoleRepository;
import com.tanle.e_commerce.Repository.Jpa.UserRepository;
import com.tanle.e_commerce.dto.PasswordChangeDTO;
import com.tanle.e_commerce.dto.RegisterUserDTO;
import com.tanle.e_commerce.dto.UserDTO;
import com.tanle.e_commerce.entities.*;
import com.tanle.e_commerce.exception.ResourceNotFoundExeption;
import com.tanle.e_commerce.mapper.UserMapper;
import com.tanle.e_commerce.respone.MessageResponse;
import com.tanle.e_commerce.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AddressRepository addressRepository;
    @Autowired
    @Lazy
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private UserMapper mapper;

    @Override
    public List<UserDTO> findAllUser() {
        return null;
    }

    @Override
    @Transactional
    public MessageResponse grantRole(Integer userId,String nameRole) {
        Role role  = roleRepository.findRoleByRoleName(nameRole.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found role"));
        MyUser myUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found user"));
        myUser.addUserRole(role);

        return MessageResponse.builder()
                .status(HttpStatus.OK)
                .message("Successfully grant role " + role.getRoleName()+ " for " + myUser.getUsername())
                .build();
    }

    @Override
    public UserDTO findById(Integer id) {
        MyUser myUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found user"));
        return mapper.convertDTO(myUser);
    }
    @Override
    public void delete(Integer id) {

    }
    @Override
    @Transactional
    public MessageResponse updateLastAccess(String username) {
        MyUser myUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found user"));
        myUser.updateLastAcess();
        return MessageResponse.builder()
                .message("Update last access successfully")
                .status(HttpStatus.OK)
                .build();
    }
    @Override
    public UserDTO update(UserDTO userDTO) {
        return null;
    }

    @Override
    @Transactional
    public MessageResponse followUser(Integer userId, Integer followingId) {
        MyUser myUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found user"));
        MyUser following = userRepository.findById(followingId)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found user"));
        myUser.followUser(following);
        userRepository.save(myUser);
        Map<String, Integer> date = Map.of(
                "Total following", myUser.countFollowing()
                ,"Total follower", myUser.countFollower());
        return MessageResponse.builder()
                .data(date)
                .message("Follower user succefully")
                .status(HttpStatus.OK)
                .build();
    }

    @Override
    @Transactional
    public MessageResponse unfollowUser(Integer userId, Integer followingId) {
        MyUser myUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found user"));
        Follower unfollower = myUser.getFollowing().stream()
                .filter(f -> f.getFollowing ().getId() == followingId && f.getUnfollowDate() == null)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found user"));
        myUser.unfollowUser(unfollower);
        Map<String, Integer> date = Map.of(
                "Total following", myUser.countFollowing()
                ,"Total follower", myUser.countFollower());
        return MessageResponse.builder()
                .data(date)
                .message("Unfollower user succefully")
                .status(HttpStatus.OK)
                .build();
    }

    @Override
    @Transactional
    public UserDTO addAddress(Integer userId, Address address) {
        MyUser myUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found user"));
        myUser.addAddress(address);
        userRepository.save(myUser);
        return mapper.convertDTO(myUser);
    }
    @Override
    @Transactional
    public MessageResponse updateAddress(Address address) {
        Address addressDB = addressRepository.save(address);
        return MessageResponse.builder()
                .status(HttpStatus.OK)
                .message("Successfully update")
                .data(addressDB)
                .build();
    }

    @Override
    @Transactional
    public MessageResponse deleteAddress(Integer userId, Integer addressId) {
        Address  addressDB = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found address"));
        MyUser myUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found user"));
        myUser.getAddresses().remove(addressDB);
        addressRepository.delete(addressDB);

        return MessageResponse.builder()
                .message("Successfully delete Address")
                .status(HttpStatus.OK)
                .data(addressDB)
                .build();
    }
    @Override
    @Transactional
    public UserDTO registerUser(RegisterUserDTO registerUserDTO) {
        MyUser myUser = MyUser.builder()
                .username(registerUserDTO.getUsername())
                .password(passwordEncoder.encode(registerUserDTO.getPassword()))
                .firstName(registerUserDTO.getFirstName())
                .lastName(registerUserDTO.getLastName())
                .phoneNumber(registerUserDTO.getPhoneNumber())
                .sex(registerUserDTO.isSex())
                .email(registerUserDTO.getEmail())
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        Role role = roleRepository.findRoleByRoleName("CUSTOMER")
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found role"));
        myUser.addUserRole(role);
        userRepository.save(myUser);

        return myUser.convertDTO();
    }

    @Override
    @Transactional
    public MessageResponse changePassword(Authentication authentication, PasswordChangeDTO passwordChangeDTO) {
      try {
          if(authentication.getName() != passwordChangeDTO.getUsername())
              throw new BadCredentialsException("Username/password invalid");
          MyUser myUser = userRepository.findByUsername(passwordChangeDTO.getUsername()).get();

          if(!passwordEncoder.matches(passwordChangeDTO.getOldPassword(), myUser.getPassword()))
              throw new BadCredentialsException("Username/password invalid");

          myUser.setPassword(passwordEncoder.encode(passwordChangeDTO.getNewPassword()));
          userRepository.save(myUser);
          return MessageResponse.builder()
                  .message("Change password successfully")
                  .status(HttpStatus.OK)
                  .build();

      }catch (BadCredentialsException e) {
          throw new BadCredentialsException(e.getMessage());
      }
    }

    @Override
    public UserDTO findByUsername(String username) {
        MyUser myUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found user"));
        return myUser.convertDTO();
    }

    @Override
    public boolean userOwnEntity(Integer id, String username) {
        MyUser myUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found user"));
        return myUser.getUsername().equals(username);
    }
}
