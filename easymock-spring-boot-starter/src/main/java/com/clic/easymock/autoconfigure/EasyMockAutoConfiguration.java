package com.clic.easymock.autoconfigure;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Configuration
@ConditionalOnProperty(prefix = "easymock", name = "enabled", havingValue = "true", matchIfMissing = false)
@AutoConfigureAfter(AopAutoConfiguration.class)
@EnableConfigurationProperties(EasyMockProperties.class)
@ComponentScan(basePackages = "com.clic.easymock")
@EnableJpaRepositories(
        basePackages = "com.clic.easymock.repository",
        entityManagerFactoryRef = "easymockEntityManagerFactory",
        transactionManagerRef = "easymockTransactionManager"
)
public class EasyMockAutoConfiguration {

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
            EntityManagerFactoryBuilder builder,
            @Qualifier("easymockDataSource") DataSource dataSource) {
        Map<String, Object> jpaProperties = new HashMap<>();
        jpaProperties.put("hibernate.hbm2ddl.auto", "update");
        jpaProperties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        jpaProperties.put("hibernate.show_sql", "false");

        return builder
                .dataSource(dataSource)
                .packages("com.clic.easymock.model")
                .persistenceUnit("easymock")
                .properties(jpaProperties)
                .build();
    }

    @Bean("easymockTransactionManager")
    public PlatformTransactionManager easymockTransactionManager(
            @Qualifier("easymockEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
