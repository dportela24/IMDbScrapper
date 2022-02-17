package com.diogo_portela.imdb_craper.configuration

import com.diogo_portela.imdb_craper.model.JSoupConnection
import org.jsoup.Jsoup
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JSoupConfiguration{
    @Bean
    fun jSoupConnection() = JSoupConnection("http://www.imdb.com")
        .header("Accept-Language", "en")
        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.66 Safari/537.36")


}