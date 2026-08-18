package com.workoutsmart.config;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Serve media bài tập (GIF/ảnh 180×180) từ exercises-dataset/ qua /media/** — không upload mới. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String mediaLocation;

    public WebConfig(@Value("${app.exercises-media-path:../exercises-dataset/}") String mediaPath) {
        this.mediaLocation = Path.of(mediaPath).toAbsolutePath().toUri().toString();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/media/**")
                .addResourceLocations(mediaLocation);
    }
}
