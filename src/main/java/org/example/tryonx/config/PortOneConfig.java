package org.example.tryonx.config;

import com.siot.IamportRestClient.IamportClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PortOneConfig {

    @Value("${imp.access}")
    private String apiKey;
    @Value("${imp.secret}")
    private String secretKey;

    //포트원 객체에다 내 액세스,시크릿 키를 넣어 내 가맹정 점보로 초기화 시킨 것
    @Bean
    public IamportClient iamportClient(){
        return new IamportClient(apiKey,secretKey);
    }


}
