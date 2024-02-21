package org.openelisglobal.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.validator.GenericValidator;
import org.jasypt.util.text.AES256TextEncryptor;
import org.jasypt.util.text.TextEncryptor;
import org.openelisglobal.config.condition.ConditionalOnProperty;
import org.openelisglobal.security.KeystoreUtil.KeyCertPair;
import org.openelisglobal.security.login.BasicAuthFilter;
import org.openelisglobal.security.login.CustomAuthenticationFailureHandler;
import org.openelisglobal.security.login.CustomFormAuthenticationSuccessHandler;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.core.Saml2X509Credential.Saml2X509CredentialType;
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.multipart.support.MultipartFilter;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

@EnableWebSecurity
@Configuration
public class SecurityConfig {

    // TODO should we move these to the properties files?
    // pages that have special security constraints
    public static final String[] OPEN_PAGES = { "/pluginServlet", "/ChangePasswordLogin",
            "/UpdateLoginChangePassword", "/health", "/rest/open-configuration-properties", "/pages" };
    public static final String[] LOGIN_PAGES = {"/LoginPage", "/ValidateLogin", "/session" };

    public static final String[] AUTH_OPEN_PAGES = { "/Home", "/Dashboard", "/Logout", "/MasterListsPage",
            "/analyzer/runAction" };
    public static final String[] RESOURCE_PAGES = { "/fontawesome-free-5.13.1-web", "/select2", "/css",
            "/favicon", "/images", "/documentation", "/scripts", "/jsp", "/pages" };
    // public static final String[] HTTP_BASIC_SERVLET_PAGES = {
    // "/pluginServlet/**", "/importAnalyzer", "/fhir/**" };
    public static final String[] REST_CONTROLLERS = { "/Provider", "/rest" };
    // public static final String[] CLIENT_CERTIFICATE_PAGES = {};

    private static final String CONTENT_SECURITY_POLICY = "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval';"
            + " connect-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline';"
            + " frame-src *.openlmis.org 'self'; object-src 'self';";

    @Value("${encryption.general.password:dev}")
    private String encryptionPassword;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    public void configureGlobalSecurity(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(authenticationProvider());
    }

    @Bean("testSecurityChain")
    @Order(-1)
    public SecurityFilterChain testSecurityChain(HttpSecurity http, HandlerMappingIntrospector introspector) throws Exception {
        CharacterEncodingFilter filter = new CharacterEncodingFilter();
        filter.setEncoding("UTF-8");
        filter.setForceEncoding(true);
        http.addFilterBefore(filter, CsrfFilter.class);
        MultipartFilter multipartFilter = new MultipartFilter();
        multipartFilter.setServletContext(SpringContext.getBean(ServletContext.class));
        http.addFilterBefore(multipartFilter, CsrfFilter.class);
        http.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());;
        // for all requests going to open pages, use this security configuration
        return http.build();
    }

    @Bean("openSecurityChain")
    @Order(0)
    public SecurityFilterChain openSecurityChain(HttpSecurity http, HandlerMappingIntrospector introspector) throws Exception {
        CharacterEncodingFilter filter = new CharacterEncodingFilter();
        filter.setEncoding("UTF-8");
        filter.setForceEncoding(true);
        http.addFilterBefore(filter, CsrfFilter.class);
        MultipartFilter multipartFilter = new MultipartFilter();
        multipartFilter.setServletContext(SpringContext.getBean(ServletContext.class));
        http.addFilterBefore(multipartFilter, CsrfFilter.class);
        String[] openPages = ArrayUtils.addAll(OPEN_PAGES, RESOURCE_PAGES);
        RequestMatcher[] matchers = Arrays.asList(openPages).stream().map(e ->(new MvcRequestMatcher.Builder(introspector)).pattern(e)).collect(Collectors.toList()).toArray(new RequestMatcher[0]);
        http.securityMatchers(securityMatchers -> securityMatchers.requestMatchers(matchers)).authorizeHttpRequests(authorize -> authorize.requestMatchers(matchers).permitAll())
            // disable csrf as it is not needed for open pages
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()
                    .contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY))));
        // for all requests going to open pages, use this security configuration
        return http.build();
    }


    @Bean("authOpenSecurityChain")
    @Order(1)
    public SecurityFilterChain authOpenSecurityChain(HttpSecurity http, HandlerMappingIntrospector introspector) throws Exception {
        CharacterEncodingFilter filter = new CharacterEncodingFilter();
        filter.setEncoding("UTF-8");
        filter.setForceEncoding(true);
        http.addFilterBefore(filter, CsrfFilter.class);
        MultipartFilter multipartFilter = new MultipartFilter();
        multipartFilter.setServletContext(SpringContext.getBean(ServletContext.class));
        http.addFilterBefore(multipartFilter, CsrfFilter.class);
        RequestMatcher[] matchers = Arrays.asList(AUTH_OPEN_PAGES).stream().map(e ->(new MvcRequestMatcher.Builder(introspector)).pattern(e)).collect(Collectors.toList()).toArray(new RequestMatcher[0]);
        http.securityMatchers(securityMatchers -> securityMatchers.requestMatchers(matchers)).authorizeHttpRequests(authorize -> authorize.requestMatchers(AUTH_OPEN_PAGES).authenticated())
            // disable csrf as it is not needed for open pages
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()
                    .contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY))));
        // for all requests going to open pages, use this security configuration
        return http.build();
    }

    @Bean("httpBasicSecurityChain")
    @Order(2)
    @ConditionalOnProperty(property = "org.itech.login.basic", havingValue = "true", matchIfMissing = true)
    public SecurityFilterChain httpBasicSecurityChain(HttpSecurity http) throws Exception {
        CharacterEncodingFilter filter = new CharacterEncodingFilter();
        filter.setEncoding("UTF-8");
        filter.setForceEncoding(true);
        http.addFilterBefore(filter, CsrfFilter.class);
        MultipartFilter multipartFilter = new MultipartFilter();
        multipartFilter.setServletContext(SpringContext.getBean(ServletContext.class));
        http.addFilterBefore(multipartFilter, CsrfFilter.class);
        // for all requests going to a http basic page, use this security configuration
        http.securityMatcher(new BasicAuthRequestedMatcher())
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                // disable csrf as it is not needed for open pages
                .csrf(csrf -> csrf.disable())
                .addFilterAt(SpringContext.getBean(BasicAuthFilter.class), BasicAuthenticationFilter.class)
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()
                        .contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY))));
        return http.build();
    }

    @Value("${org.itech.login.saml.registrationId:keycloak}")
    private String registrationId;
    @Value("${org.itech.login.saml.entityId:OpenELIS-Global_saml}")
    private String entityId;

    @Value("${server.ssl.key-store}")
    private Resource keyStore;
    @Value("${server.ssl.key-store-password}")
    private String keyStorePassword;

    @Value("${org.itech.login.saml.metadatalocation:}")
    private String metadata;

    @Value("${org.itech.login.saml.idpEntityId:}")
    private String idpEntityId;
    @Value("${org.itech.login.saml.webSSOEndpoint:}")
    private String webSSOEndpoint;
    @Value("${org.itech.login.saml.idpVerificationCertificateLocation:/run/secrets/samlIDP.crt}")
    private String idpVerificationCertificateLocation;

    @Bean("samlRelyingPartyRegistrationRepository")
    @ConditionalOnProperty(property = "org.itech.login.saml", havingValue = "true")
    public RelyingPartyRegistrationRepository relyingPartyRegistrationRepository() throws KeyStoreException,
            CertificateException, NoSuchAlgorithmException, IOException, UnrecoverableKeyException {
        RelyingPartyRegistration relyingPartyRegistration;
        final String acsUrlTemplate = "{baseUrl}/login/saml2/sso/{registrationId}";

        KeyStore keystore = KeystoreUtil.readKeyStoreFile(keyStore, keyStorePassword.toCharArray());
        KeyCertPair keyCert = KeystoreUtil.getKeyCertFromKeystore(keystore, keyStorePassword.toCharArray());
        Saml2X509Credential credential = Saml2X509Credential.signing(keyCert.getKey(),
                (X509Certificate) keyCert.getCert());
        if (GenericValidator.isBlankOrNull(metadata)) {
            Saml2X509Credential idpVerificationCertificate;
            try (InputStream pub = new FileInputStream(new File(idpVerificationCertificateLocation))) {
                X509Certificate c = (X509Certificate) CertificateFactory.getInstance("X.509")
                        .generateCertificate(pub);
                idpVerificationCertificate = new Saml2X509Credential(c, Saml2X509CredentialType.VERIFICATION);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            relyingPartyRegistration = RelyingPartyRegistration.withRegistrationId(registrationId)//
                    .assertionConsumerServiceLocation(acsUrlTemplate)//
                    .signingX509Credentials(e -> e.add(credential))//
                    .assertingPartyDetails(config -> config.entityId(idpEntityId)//
                            .singleSignOnServiceLocation(webSSOEndpoint)//
                            .singleLogoutServiceLocation(webSSOEndpoint)//
                            .wantAuthnRequestsSigned(true)//
                            .verificationX509Credentials(c -> c.add(idpVerificationCertificate)))//
                    .entityId(entityId)//
                    .build();
        } else {
            relyingPartyRegistration = RelyingPartyRegistrations.fromMetadataLocation(metadata)//
                    .registrationId(registrationId)//
                    .assertionConsumerServiceLocation(acsUrlTemplate)//
                    .signingX509Credentials(e -> e.add(credential))//
                    .entityId(entityId)//
                    .build();
        }

        // SAML configuration
        // Mapping this application to one or more Identity Providers
        return new InMemoryRelyingPartyRegistrationRepository(relyingPartyRegistration);
    }

    @Bean("saml2SecurityChain")
    @ConditionalOnProperty(property = "org.itech.login.saml", havingValue = "true")
    @Order(3)
    public SecurityFilterChain saml2SecurityChain(HttpSecurity http) throws Exception {
        CharacterEncodingFilter filter = new CharacterEncodingFilter();
        filter.setEncoding("UTF-8");
        filter.setForceEncoding(true);
        http.addFilterBefore(filter, CsrfFilter.class);
        MultipartFilter multipartFilter = new MultipartFilter();
        multipartFilter.setServletContext(SpringContext.getBean(ServletContext.class));
        http.addFilterBefore(multipartFilter, CsrfFilter.class);

        // for all requests going to a saml2 page, use this security configuration
        http.securityMatcher(new SamlRequestedMatcher())
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .saml2Logout(Customizer.withDefaults())
                .saml2Login(saml2Login -> {
                    try {
                        saml2Login
                                .failureHandler(samlAuthenticationFailureHandler())
                                .successHandler(samlAuthenticationSuccessHandler())
                                .relyingPartyRegistrationRepository(relyingPartyRegistrationRepository());
                    } catch (UnrecoverableKeyException | KeyStoreException | CertificateException
                            | NoSuchAlgorithmException | IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                })
                // disable csrf as it is not needed for open pages
                .csrf(csrf -> csrf.disable())
                .addFilterAt(SpringContext.getBean(BasicAuthFilter.class), BasicAuthenticationFilter.class)
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()
                        .contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY))));
        return http.build();

    }

    @Bean("samlAuthenticationSuccessHandler")
    @ConditionalOnProperty(property = "org.itech.login.saml", havingValue = "true")
    public AuthenticationSuccessHandler samlAuthenticationSuccessHandler() {
        return new CustomFormAuthenticationSuccessHandler();
    }

    @Bean("samlAuthenticationFailureHandler")
    @ConditionalOnProperty(property = "org.itech.login.saml", havingValue = "true")
    public AuthenticationFailureHandler samlAuthenticationFailureHandler() {
        return new CustomAuthenticationFailureHandler();
    }

    @Value("${org.itech.login.oauth.config:}")
    private String config;
    @Value("${org.itech.login.oauth.clientID:OpenELIS-Global_oauth}")
    private String clientID;
    @Value("${org.itech.login.oauth.clientSecret:}")
    private String clientSecret;

    private ClientRegistration clientRegistrationFromConfig(String config) {
        return ClientRegistrations.fromOidcIssuerLocation(config).clientId(clientID).clientSecret(clientSecret)
                .build();
    }

    @Bean("oauthClientRegistrationRepository")
    @ConditionalOnProperty(property = "org.itech.login.oauth", havingValue = "true")
    public ClientRegistrationRepository clientRegistrationRepository() {
        List<ClientRegistration> registrations = new ArrayList<>();

        if (!GenericValidator.isBlankOrNull(config)) {
            registrations.add(clientRegistrationFromConfig(config));
        }
        return new InMemoryClientRegistrationRepository(registrations);
    }

    @Bean("oauth2SecurityChain")
    @Order(4)
    @ConditionalOnProperty(property = "org.itech.login.oauth", havingValue = "true")
    public SecurityFilterChain oauth2SecurityChain(HttpSecurity http) throws Exception {
        CharacterEncodingFilter filter = new CharacterEncodingFilter();
        filter.setEncoding("UTF-8");
        filter.setForceEncoding(true);
        http.addFilterBefore(filter, CsrfFilter.class);
        MultipartFilter multipartFilter = new MultipartFilter();
        multipartFilter.setServletContext(SpringContext.getBean(ServletContext.class));
        http.addFilterBefore(multipartFilter, CsrfFilter.class);
        // for all requests going to a oauth2 page, use this security configuration
        http.securityMatcher(new OAuthRequestedMatcher())
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .oauth2Login(oAuth2 -> oAuth2.clientRegistrationRepository(clientRegistrationRepository())//
                        .authorizedClientService(authorizedClientService())
                        .failureHandler(oauthAuthenticationFailureHandler())
                        .successHandler(oauthAuthenticationSuccessHandler()))
                .logout(logout -> logout.logoutSuccessHandler(oidcLogoutSuccessHandler()));
        return http.build();
    }

    private LogoutSuccessHandler oidcLogoutSuccessHandler() {
        OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler = new OidcClientInitiatedLogoutSuccessHandler(
                clientRegistrationRepository());
        return oidcLogoutSuccessHandler;
    }

    @Bean
    @ConditionalOnProperty(property = "org.itech.login.oauth", havingValue = "true")
    public OAuth2AuthorizedClientService authorizedClientService() {
        return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository());
    }

    @Bean("oauthAuthenticationSuccessHandler")
    @ConditionalOnProperty(property = "org.itech.login.oauth", havingValue = "true")
    public AuthenticationSuccessHandler oauthAuthenticationSuccessHandler() {
        return new CustomFormAuthenticationSuccessHandler();
    }

    @Bean("oauthAuthenticationFailureHandler")
    @ConditionalOnProperty(property = "org.itech.login.oauth", havingValue = "true")
    public AuthenticationFailureHandler oauthAuthenticationFailureHandler() {
        return new CustomAuthenticationFailureHandler();
    }

    @Bean("certSecurityChain")
    @ConditionalOnProperty(property = "org.itech.login.certificate", havingValue = "true")
    @Order(5)
    public SecurityFilterChain certSecurityChain(HttpSecurity http) throws Exception {
        CharacterEncodingFilter filter = new CharacterEncodingFilter();
        filter.setEncoding("UTF-8");
        filter.setForceEncoding(true);
        http.addFilterBefore(filter, CsrfFilter.class);
        // for all requests going to a oauth2 page, use this security configuration
        http.securityMatcher(new CertificateAuthRequestedMatcher())
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .x509(x509 -> x509.subjectPrincipalRegex("CN=(.*?)(?:,|$)")
                        .userDetailsService(SpringContext.getBean(UserDetailsService.class)))
                .csrf(csrf -> csrf.disable());
        return http.build();
    }

    @Bean("defaultSecurityChain")
    @Order(Ordered.LOWEST_PRECEDENCE)
    public SecurityFilterChain defaultSecurityChain(HttpSecurity http, HandlerMappingIntrospector introspector) throws Exception {
        CharacterEncodingFilter filter = new CharacterEncodingFilter();
        filter.setEncoding("UTF-8");
        filter.setForceEncoding(true);
        http.addFilterBefore(filter, CsrfFilter.class);
        MultipartFilter multipartFilter = new MultipartFilter();
        multipartFilter.setServletContext(SpringContext.getBean(ServletContext.class));
        http.addFilterBefore(multipartFilter, CsrfFilter.class);
        
        RequestMatcher[] matchers = Arrays.asList(LOGIN_PAGES).stream().map(e -> (RequestMatcher) (new MvcRequestMatcher.Builder(introspector)).pattern(e)).collect(Collectors.toList()).toArray(new RequestMatcher[0]);
        http.authorizeHttpRequests(authorize -> authorize.requestMatchers(matchers)
            .permitAll()
            );
            //.formLogin(login -> login.loginPage("/LoginPage").permitAll().loginProcessingUrl("/ValidateLogin")
            //    .usernameParameter("loginName").passwordParameter("password")
            //    .failureHandler(customAuthenticationFailureHandler())
            //    .successHandler(customAuthenticationSuccessHandler()))
            // setup logout
            //.logout(logout -> logout.logoutUrl("/Logout").logoutSuccessUrl("/LoginPage")
            //    .invalidateHttpSession(true))
            //.sessionManagement(sessionManagement -> sessionManagement.invalidSessionUrl("/LoginPage")
            //    .sessionFixation().migrateSession())
            //.csrf(csrf -> csrf.ignoringRequestMatchers("/ValidateLogin"))
            // add security headers
            //.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()
            //    .contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY))));
        return http.build();
    }

    @Bean
    public AuthenticationFailureHandler customAuthenticationFailureHandler() {
        return new CustomAuthenticationFailureHandler();
    }

    @Bean
    @Primary
    public AuthenticationSuccessHandler customAuthenticationSuccessHandler() {
        return new CustomFormAuthenticationSuccessHandler();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    // @Bean
    // public static UserDetailsService allowAllUserDetailsService() {
    // return new UserDetailsService() {
    // @Override
    // public UserDetails loadUserByUsername(String username) {
    // return new User("falseIdol", "", new ArrayList<>());
    // }
    // };
    // }

    // @Bean
    // @ConditionalOnProperty(property = "org.itech.authProvider.useADLDAP",
    // havingValue = "true")
    // public AuthenticationProvider activeDirectoryLdapAuthenticationProvider() {
    // LdapAuthenticationProvider adProvider = new LdapAuthenticationProvider(new
    // LdapAuthenticator()
    //
    // return adProvider;
    // }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        return authenticationProvider;
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public TextEncryptor textEncryptor() {
        AES256TextEncryptor textEncryptor = new AES256TextEncryptor();
        textEncryptor.setPassword(encryptionPassword);
        return textEncryptor;
    }

    // @Bean
    // public AuthenticationEventPublisher authenticationEventPublisher
    // (ApplicationEventPublisher applicationEventPublisher) {
    // return new DefaultAuthenticationEventPublisher(applicationEventPublisher);
    // }

    private static class OAuthRequestedMatcher implements RequestMatcher {
        @Override
        public boolean matches(HttpServletRequest request) {
            String auth = request.getHeader("Authorization");
            boolean haveOauth2Token = (auth != null) && auth.startsWith("Bearer");
            boolean useOauth = haveOauth2Token || "true".equals(request.getParameter("useOAUTH"))
                    || request.getRequestURI().contains("oauth");
            return useOauth;
        }
    }

    private static class SamlRequestedMatcher implements RequestMatcher {
        @Override
        public boolean matches(HttpServletRequest request) {
            String auth = request.getHeader("Authorization");
            boolean useSAML = (auth != null) && auth.startsWith("SAML")
                    || "true".equals(request.getParameter("useSAML")) || request.getRequestURI().contains("saml2");
            return useSAML;
        }
    }

    private static class BasicAuthRequestedMatcher implements RequestMatcher {
        @Override
        public boolean matches(HttpServletRequest request) {
            String auth = request.getHeader("Authorization");
            boolean haveBasicAuth = (auth != null) && auth.startsWith("Basic");
            return haveBasicAuth;
        }
    }

    private static class CertificateAuthRequestedMatcher implements RequestMatcher {
        @Override
        public boolean matches(HttpServletRequest request) {
            String auth = request.getHeader("Authorization");
            boolean haveCertificateAuth = (auth != null) && auth.startsWith("Cert");
            return haveCertificateAuth;
        }
    }
}