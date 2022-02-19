package com.diogo_portela.imdb_craper.configuration

import com.diogo_portela.imdb_craper.model.JSoupConnection
import org.jsoup.Jsoup
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JSoupConfiguration{
    @Bean
    fun jSoupConnection() = JSoupConnection("http://www.imdb.com")
        .header("Accept-Language", "en-US")
        .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.80 Safari/537.36")


}