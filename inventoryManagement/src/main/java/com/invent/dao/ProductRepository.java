package com.invent.dao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.invent.entities.Product;
import com.invent.entities.User;

public interface ProductRepository extends JpaRepository<Product, Integer> {
	
	
	
	
	@Query("from Product as c where c.user.id=:userId")
	//it has current page 
	//product per page
	public Page<Product> findProductsByUser(@Param("userId")int userId, Pageable pageable);
	
	//searching is here
	public List<Product> findByNameContainingAndUser(String n, User user);
	

}
