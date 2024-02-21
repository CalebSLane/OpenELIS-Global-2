package org.openelisglobal.config;

import jakarta.persistence.EntityManagerFactory;

import java.util.Properties;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
public class HibernateConfig {

    static JpaTransactionManager transactionManager;
    static LocalContainerEntityManagerFactoryBean emf;

    @Autowired
    private DataSource dataSource;

    @Bean
    @DependsOn("liquibase")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        if (emf == null) {
            emf = new LocalContainerEntityManagerFactoryBean();
            emf.setDataSource(dataSource);

            JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
            emf.setJpaVendorAdapter(vendorAdapter);

            emf.setJpaProperties(additionalProperties());
            emf.setPersistenceXmlLocation("classpath:persistence/persistence.xml");
//            activate this once we migrate away from hbm.xmls and persistence.xml
//            emf.setPackagesToScan("org.openelisglobal");
        }

        return emf;
    }

    Properties additionalProperties() {
        Properties properties = new Properties();
        properties.setProperty("hibernate.default_schema", "clinlims");
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        properties.setProperty("show_sql", "false");
		properties.setProperty("hibernate.format_sql", "true");
		properties.setProperty("hibernate.jdbc.batch_size", "50");
		properties.setProperty("hibernate.jdbc.batch_versioned_data", "true");
		properties.setProperty("hibernate.query.factory_class", "org.hibernate.hql.internal.classic.ClassicQueryTranslatorFactory");
		properties.setProperty("hibernate.cache.provider_class", "org.hibernate.cache.NoCacheProvider");
		properties.setProperty("hibernate.cache.use_second_level_cache", "false");
		properties.setProperty("hibernate.cache.use_query_cache", "false");
		properties.setProperty("hibernate.current_session_context_class", "thread");
           
        return properties;
    }

    @Bean("transactionManager")
    @Primary
    public PlatformTransactionManager getTransactionManager(EntityManagerFactory entityManagerFactory) {
        if (transactionManager == null) {
            transactionManager = new JpaTransactionManager();
            transactionManager.setEntityManagerFactory(entityManagerFactory);
        }
        return transactionManager;
    }

}
