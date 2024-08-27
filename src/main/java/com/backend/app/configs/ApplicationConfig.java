package com.backend.app.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.transaction.PlatformTransactionManager;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.orm.jpa.JpaTransactionManager;

@Configuration
public class ApplicationConfig {

    // @Bean
    // public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
    //     return new JpaTransactionManager(entityManagerFactory);
    // }

    // @Bean
    // public RedisConnectionFactory jedisConnectionFactory() {
    //     RedisStandaloneConfiguration config = new RedisStandaloneConfiguration("localhost", 6380);
    //     config.setPassword("d4B$8fF@z1N#kPq!L9wT3vX%7gR5hY2sJ6mU+0aE4cI^");
    //     return new JedisConnectionFactory(config);
    // }
    // @Bean
    // public RedisTemplate<Object, Object> redisTemplate() {
    //     RedisTemplate<Object, Object> template = new RedisTemplate<>();
        
    //     ObjectMapper objectMapper = new ObjectMapper();

    //     // Set date format
    //     objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    //     objectMapper.registerModule(new JavaTimeModule());

    //     Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer(objectMapper, Object.class);
    //     template.setKeySerializer(new StringRedisSerializer());
    //     template.setValueSerializer(serializer);
      
    //     template.setHashKeySerializer(new StringRedisSerializer());
    //     template.setHashValueSerializer(serializer);

    //     template.setConnectionFactory(jedisConnectionFactory());
    //     template.afterPropertiesSet();
    //     return template;
    // }
}
