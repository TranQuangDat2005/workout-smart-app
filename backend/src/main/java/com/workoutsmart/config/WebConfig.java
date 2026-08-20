package com.workoutsmart.config;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Serve media bài tập: kho hệ thống qua /media/**, upload người dùng qua /media/user/**. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String mediaLocation;
    private final String userMediaLocation;

    public WebConfig(@Value("${app.exercises-media-path:../exercises-dataset/}") String mediaPath,
                     @Value("${app.user-media-path:./uploads/media/}") String userMediaPath) {
        this.mediaLocation = Path.of(mediaPath).toAbsolutePath().toUri().toString();
        this.userMediaLocation = Path.of(userMediaPath).toAbsolutePath().toUri().toString();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/media/user/**")
                .addResourceLocations(userMediaLocation);
        registry.addResourceHandler("/media/**")
                .addResourceLocations(mediaLocation);
    }
}
