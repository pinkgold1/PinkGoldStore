package com.pinkgold.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${upload.dir:images}")
    private String uploadDir;

    /**
     * Serve ảnh tĩnh từ thư mục images/ qua URL http://localhost:8080/images/ten-file.jpg
     * Điều này cho phép frontend dùng <img src="http://localhost:8080/images/abc.jpg">
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();

        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + uploadPath + "/");
    }
}