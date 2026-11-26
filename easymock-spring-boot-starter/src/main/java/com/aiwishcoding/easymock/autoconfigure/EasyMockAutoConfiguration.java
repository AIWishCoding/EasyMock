package com.aiwishcoding.easymock.autoconfigure;

import com.alibaba.druid.pool.DruidDataSource;
import com.aiwishcoding.easymock.aop.MockStatusInterceptor;
import com.aiwishcoding.easymock.security.EasyMockAdminFilter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.context.annotation.Conditional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Configuration
@ConditionalOnProperty(prefix = "easymock", name = "enabled", havingValue = "true", matchIfMissing = false)
@Conditional(EasyMockProfileCondition.class)
@AutoConfigureAfter(AopAutoConfiguration.class)
@EnableConfigurationProperties(EasyMockProperties.class)
@ComponentScan(basePackages = "com.aiwishcoding.easymock")
@EnableJpaRepositories(
        basePackages = "com.aiwishcoding.easymock.repository",
        entityManagerFactoryRef = "easymockEntityManagerFactory",
        transactionManagerRef = "easymockTransactionManager"
)
public class EasyMockAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "easymock", name = "admin-token")
    public FilterRegistrationBean<EasyMockAdminFilter> easymockAdminFilter(
            EasyMockProperties properties) {
        FilterRegistrationBean<EasyMockAdminFilter> registration =
                new FilterRegistrationBean<>(new EasyMockAdminFilter(properties));
        registration.addUrlPatterns(properties.getPagePath(), properties.getPagePath() + "/*",
                properties.getApiPath(), properties.getApiPath() + "/*");
        registration.setName("easyMockAdminFilter");
        registration.setOrder(Integer.MIN_VALUE + 100);
        return registration;
    }

    @Bean
    public WebMvcConfigurer easyMockStatusConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new MockStatusInterceptor());
            }
        };
    }

    @Bean("easymockDataSource")
    public DataSource easymockDataSource(EasyMockProperties properties) {
        String dbPath = properties.getDbPath();
        File dbFile = new File(dbPath);
        File parentDir = dbFile.getAbsoluteFile().getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:file:" + dbFile.getAbsolutePath() + ";AUTO_SERVER=TRUE;MODE=MySQL");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        dataSource.setInitialSize(1);
        dataSource.setMinIdle(1);
        dataSource.setMaxActive(5);
        dataSource.setValidationQuery("SELECT 1");
        return dataSource;
    }

    @Bean("easymockEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean easymockEntityManagerFactory(
            @Qualifier("easymockDataSource") DataSource dataSource) {
        Map<String, Object> jpaProperties = new HashMap<>();
        jpaProperties.put("hibernate.hbm2ddl.auto", "update");
        jpaProperties.put("hibernate.show_sql", "false");

        LocalContainerEntityManagerFactoryBean factory =
                new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPackagesToScan("com.aiwishcoding.easymock.model");
        factory.setPersistenceUnitName("easymock");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setJpaPropertyMap(jpaProperties);
        return factory;
    }

    @Bean("easymockTransactionManager")
    public PlatformTransactionManager easymockTransactionManager(
            @Qualifier("easymockEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
