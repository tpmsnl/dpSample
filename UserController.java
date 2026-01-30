package com.example.demo;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

// UserController.java
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:4200")
public class UserController {
    
    @Autowired
    private UserService userService;

    @Autowired private UserRepository ur;
    
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @PutMapping("/reorder")                                                                                                                                             
  public List<User> reorderUsers(@RequestBody List<Long> userIds) {                                                                                                   
      // Update position/order field for each user                                                                                                                    
      List<User> reorderedUsers = new ArrayList<>();                                                                                                                  
      for (int i = 0; i < userIds.size(); i++) {                                                                                                                      
          User user = userService.getUserById(userIds.get(i));                                                                                          
          user.setPosition(i);  // Add 'position' field to User entity                                                                                                
          reorderedUsers.add(ur.save(user));                                                                                                              
      }                                                                                                                                                               
      return reorderedUsers;                                                                                                                                          
  }     
    
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }
    
    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody User user) {
        User savedUser = userService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @Valid @RequestBody User userDetails) {
        User updatedUser = userService.updateUser(id, userDetails);
        return ResponseEntity.ok(updatedUser);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }
}
