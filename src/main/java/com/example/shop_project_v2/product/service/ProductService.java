package com.example.shop_project_v2.product.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.shop_project_v2.product.dto.ProductOptionRequestDTO;
import com.example.shop_project_v2.product.dto.ProductRequestDTO;
import com.example.shop_project_v2.product.dto.ProductResponseDTO;
import com.example.shop_project_v2.product.entity.Product;
import com.example.shop_project_v2.product.entity.ProductImage;
import com.example.shop_project_v2.product.entity.ProductOption;
import com.example.shop_project_v2.product.repository.ProductImageRepository;
import com.example.shop_project_v2.product.repository.ProductOptionRepository;
import com.example.shop_project_v2.product.repository.ProductRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
@Transactional
public class ProductService {
	private final ProductRepository productRepository;
	private final ProductImageRepository productImageRepository;
	private final ProductOptionRepository productOptionRepository;
	
	public Product FindById(Long id) {
		return productRepository.findById(id).orElseThrow();
	}
	
	public void CreateProduct(ProductRequestDTO productRequestDto) {
		Product product = Product.builder()
				.name(productRequestDto.getName())
				.price(productRequestDto.getPrice())
				.description(productRequestDto.getDescription())
				.build();
		
		productRepository.save(product);
		
		// 이미지 처리
		List<MultipartFile> imageFiles = productRequestDto.getImages();	
		if (imageFiles != null && !imageFiles.isEmpty()) {
			for (int i = 0; i < imageFiles.size(); ++i) {
	            MultipartFile imageFile = imageFiles.get(i);
	            String imageUrl = saveImage(imageFile);
	            
	            ProductImage productImage = new ProductImage();
	            productImage.setProduct(product);
	            productImage.setImageUrl(imageUrl);
	            productImageRepository.save(productImage);

	            // 첫 번째 이미지를 썸네일로 설정
	            if (i == 0) {
	                product.setThumbnailUrl(imageUrl);
	            }
			}
		}
		
		// 옵션 처리
		List<ProductOptionRequestDTO> optionDto = productRequestDto.getOptions();
		if (optionDto != null && !optionDto.isEmpty()) {
			for (ProductOptionRequestDTO option : optionDto) {
				ProductOption productOption = new ProductOption();
				productOption.setProduct(product);
				productOption.setColor(option.getColor());
				productOption.setSize(option.getSize());
				productOption.setStockQuantity(option.getStockQuantity());
				productOptionRepository.save(productOption);
			}
		}
		
	}
	
	private String saveImage(MultipartFile imageFile) {
		if (imageFile.isEmpty()) return null;
		
		try {
			String uploadDir = "C:/uploads/";
			String fileName = imageFile.getOriginalFilename();
			String filePath = uploadDir + fileName;
			
			Path path = Paths.get(filePath);
			Files.createDirectories(path.getParent());
			Files.write(path, imageFile.getBytes());
			
			return "/uploads/" + fileName;
			
		} catch (IOException e) {
			throw new RuntimeException("이미지 저장 중 오류 발생", e);
		}
	}
	
	public List<Product> getAllProducts() {
		return productRepository.findAll();
	}
	
//	public List<Product> getProductsByNewest() {
//		return productRepository.findAllByOrderByCreatedDateDesc();
//	}
//	
//	public List<Product> getProductsByOldest() {
//        return productRepository.findAllByOrderByCreatedDateAsc();
//    }
	
    public Page<ProductResponseDTO> getProductsSorted(String sort, int page, int size) {
    	Pageable pageable = PageRequest.of(page, size);
    	
    	Page<Product> productPage;
        if ("oldest".equals(sort)) {
        	productPage = productRepository.findAllByOrderByCreatedDateAsc(pageable);
        } else {
        	productPage = productRepository.findAllByOrderByCreatedDateDesc(pageable);
        }
        return productPage.map(product ->
        new ProductResponseDTO(
        		product.getProductId(),
                product.getName(),
                product.getPrice(),
                product.getThumbnailUrl()
		        )
		);
    }
    
    public Page<ProductResponseDTO> searchProducts(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> productPage = productRepository.findByNameContaining(keyword, pageable);

        return productPage.map(product ->
                new ProductResponseDTO(
                		product.getProductId(),
                        product.getName(),
                        product.getPrice(),
                        product.getThumbnailUrl()
                )
        );
    }
}
