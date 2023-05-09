package com.invent.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.invent.dao.ProductRepository;
import com.invent.dao.UserRepository;
import com.invent.entities.Product;
import com.invent.entities.User;
//return simple response body no view is returned
@RestController      
public class SearchController{
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private ProductRepository contactRepository;
	
	@GetMapping("/search/{query}")
	public ResponseEntity<?> search(@PathVariable("query") String query,Principal principal){
		System.out.println(query);
		
		User user= this.userRepository.getUserByUserName(principal.getName());
		List<Product> proucts=this.contactRepository.findByNameContainingAndUser(query, user);
		return ResponseEntity.ok(proucts);	}
}
	



