package com.invent.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpSession;

import org.apache.catalina.TomcatPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.invent.dao.ProductRepository;
import com.invent.dao.UserRepository;
import com.invent.entities.Product;
import com.invent.entities.User;
import com.invent.helper.Message;

@Controller
@RequestMapping("/user")
public class UserController {
	
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;
	
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private ProductRepository productRepository;
	
	@ModelAttribute
	public void addCommonData(Model model , Principal principal) {                //work for index as  well as add_product to pass user name work
													// all handlers
		String userName=principal.getName();
		System.out.println("USERNAME"+userName);
		//get the user using user name
		User user = userRepository.getUserByUserName(userName);
		System.out.println("USER"+user);
		
		model.addAttribute("user", user);
		
		
	}
	
	//this is dashboard for home
	@RequestMapping("/index")
	public String dashboard(Model model, Principal principal) {
		
/*		
		String userName=principal.getName();
		System.out.println("USERNAME"+userName);
		//get the user using user name
		User user = userRepository.getUserByUserName(userName);
		System.out.println("USER"+user);
		
		model.addAttribute("user", user);
		*/
		model.addAttribute("title","Dashboard");
		return "normal/user_dashboard";
		
	}
	
	// add form handler to open 
	@GetMapping("/add-product")
	public String openAddProductForm(Model model) {
		
		model.addAttribute("title","Add Product");
		model.addAttribute("product", new Product());
		return "normal/add_product_form";
		
	}
	
	//processing add product form
	@PostMapping("/process-product")
	public String processProduct(@ModelAttribute Product product,
			@RequestParam("productImage") MultipartFile file,  Principal principal,HttpSession session) { //principal is fetching user name
		
		try {																							//use httpsession for storing
																									// for short time 
		
		String name=principal.getName();
		User user=this.userRepository.getUserByUserName(name);
		
		//uploading file /image
		
		if(file.isEmpty()) {
			//if file is empty
			System.out.println("file is empty");
			product.setImage("product.png");
		}
		else {
			//upload file to folder and attach it with product name
			product.setImage(file.getOriginalFilename());
			File savefile = new ClassPathResource("static/img").getFile();
			//take all bytes from source and provide it to destination
			
			Path path = Paths.get(savefile.getAbsolutePath() + File.separator + file.getOriginalFilename());
			Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
			System.out.println("image is uploaded");
		}
		
		user.getProducts().add(product);
		product.setUser(user);
		
		
		this.userRepository.save(user);  //saving in database
		
		
		System.out.println("DATA"+product);
		System.out.println("added to database");
		
		//success message------
		session.setAttribute("message", new Message("---Product is added---","success"));
		
		}catch (Exception e) {
			System.out.println("error"+e.getMessage());
			e.printStackTrace();
			//error message
			session.setAttribute("message", new Message("---Something is wrong, try again---","danger"));
			
		}
		return "normal/add_product_form";
	}
	
	//show products handler
	// per page = 5
	//current page=0[page]
	@GetMapping("/show-products/{page}")
	// public String showProducts(Model m, Principal principal) {
	public String showProducts(@PathVariable("page") Integer page ,Model m, Principal principal) {	
		m.addAttribute("title","view products");
		// product list will be sent from here
	//	String userName=principal.getName();
	//	User user=this.userRepository.getUserByUserName(userName);
	//	List<Product> products=user.getProducts();
		String userName=principal.getName();
		User user=this.userRepository.getUserByUserName(userName);
		//current page an product per page
		Pageable pageable = PageRequest.of(page, 2);
		Page<Product> products=this.productRepository.findProductsByUser(user.getId(),pageable);
		m.addAttribute("products", products);
		m.addAttribute("currentPage",page);
		m.addAttribute("totalPages",products.getTotalPages());
		return "normal/show_products";
	}
	// specific product details
	@RequestMapping("/{pId}/product")
	public String showProductDetail(@PathVariable("pId") Integer pId, Model model, Principal principal) {
		System.out.println("cid"+pId);
		
		Optional<Product> productOptional=this.productRepository.findById(pId);
		Product product=productOptional.get();
		
		//
		String userName=principal.getName();
		User user=this.userRepository.getUserByUserName(userName);
		if(user.getId()==product.getUser().getId()) {
			
			
			model.addAttribute("product",product);
			model.addAttribute("title",product.getName());
		}
		return "/normal/product_detail";
	}
	
	//deleting items
	@GetMapping("/delete/{pid}")
	public String deleteProduct(@PathVariable("pid") Integer pId,Model model,HttpSession session
			,Principal principal)
	{
		Product product=this.productRepository.findById(pId).get();
		
		
		//security so other can not do this
		
		//product.setUser(null);
	User user=this.userRepository.getUserByUserName(principal.getName());
		user.getProducts().remove(product);
		this.userRepository.save(user);
		System.out.println("DELETED");
		session.setAttribute("message", new Message("Product deleted succesfully", "success"));
		return "redirect:/user/show-products/0";
	}
	
	// update page product details
	@PostMapping("/update-product/{pid}")
	public String updateForm(@PathVariable("pid") Integer pid,Model m)
	{
		
		m.addAttribute("title","Update product");
		
		Product product=this.productRepository.findById(pid).get();
		m.addAttribute("product",product);
		return "normal/update_form";
	}
	
	//editing product details
	@RequestMapping(value="/process-update", method=RequestMethod.POST)
	public String updateHandler(@ModelAttribute Product product,@RequestParam("productImage")
	MultipartFile file, Model m , HttpSession session,Principal principal) {
		
		
		try {
			
			//old product details
			Product oldproductDetails=this.productRepository.findById(product.getpId()).get();
			
			//image part
			if(!file.isEmpty()) {
				//add new image file
				//delete old image and add new img
				File deleteFile = new ClassPathResource("static/img").getFile();
				File file1=new File(deleteFile, oldproductDetails.getImage());
				file1.delete();
				
				
				//new img
				File savefile = new ClassPathResource("static/img").getFile();
				//take all bytes from source and provide it to destination
				
				Path path = Paths.get(savefile.getAbsolutePath() + File.separator +
						file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				product.setImage(file.getOriginalFilename());
				
				
			}else {
				product.setImage(oldproductDetails.getImage());
			}
			
			User user= this.userRepository.getUserByUserName(principal.getName());
			product.setUser(user);
			this.productRepository.save(product);
			session.setAttribute("message", new Message("product details is updated","success"));
			
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		System.out.println("Product"+product.getName());
		System.out.println("Product"+product.getpId());
		
		return "redirect:/user/"+product.getpId()+"/product";
	}
	
	// my profile section
	@GetMapping("/profile")
	public String yourProfile(Model model) {
		model.addAttribute("title", "My Profile");
		return "normal/profile";
		
		
	}
	
	//for setting change password
	@GetMapping("/settings")
	public String openSettings() {
			return "normal/settings";
		}
	// change update password
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("oldPassword") String oldPassword, 
			@RequestParam("newPassword") String newPassword, Principal principal
			, HttpSession session) {
		
		System.out.println("OLD PASSWORD"+oldPassword);
		System.out.println("new PASSWORD"+newPassword);
		
		String userName=principal.getName();
		User currentUser=this.userRepository.getUserByUserName(userName);
		System.out.println(currentUser.getPassword());
		
		if(this.bCryptPasswordEncoder.matches(oldPassword, currentUser.getPassword())) {
		
			currentUser.setPassword(this.bCryptPasswordEncoder.encode(newPassword));
			this.userRepository.save(currentUser);
			
			session.setAttribute("message", new Message("Password changed","success"));
			
			
		}else {
			
			session.setAttribute("message", new Message("wrong login credentials","danger"));
			return "redirect:/user/settings";
		}
		
		
		return "redirect:/user/settings";
	}
	

}
