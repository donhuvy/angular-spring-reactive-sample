package com.example.demo;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;
import static org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication;

@SpringBootTest
public class ApplicationTests {

    @Autowired
    ApplicationContext context;

    WebTestClient client;

    @BeforeEach
    public void setup() {
        client = WebTestClient
                .bindToApplicationContext(context)
                .apply(springSecurity())
                .configureClient()
                //.defaultHeaders( h-> h.setBasicAuth("user", "password"))
                //.filter(basicAuthentication())
                .build();
    }

    @Test
    public void getAllPostsWithAuthentication_ShouldBeOk() {
        client
                .get()
                .uri("/posts/")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void getNoneExistedPost_ShouldReturn404() {
        client
                .get()
                .uri("/posts/ABC")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    public void createPostWithoutAuthentication_shouldReturn401() {
        client
                .post()
                .uri("/posts")
                .body(BodyInserters.fromObject(Post.builder().title("Post test").content("content of post test").build()))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void updateNoneExistedPostWithUserRole_shouldReturn404() {
        client
                .mutate().filter(basicAuthentication("user", "password")).build()
                .put()
                .uri("/posts/none_existed")
                .body(BodyInserters.fromObject(Post.builder().title("updated title").content("updated content").build()))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    public void deletePostWithUserRole_shouldReturn403() {
        client
                .mutate().filter(basicAuthentication("user", "password")).build()
                .delete()
                .uri("/posts/1")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void deleteNoneExistedPostWithAdminRole_shouldReturn404() {
        client
                .mutate().filter(basicAuthentication("admin", "password")).build()
                .delete()
                .uri("/posts/none_existed")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    //@WithMockUser
    public void postCrudOperations() {
        int randomInt = new Random().nextInt();
        String title = "Post test " + randomInt;
        String content = "content of " + title;


        var result = client
                .mutate().filter(basicAuthentication("user", "password")).build()
                .post()
                .uri("/posts")
                .bodyValue(Post.builder().title(title).content(content).build())
                .exchange()
                .expectStatus().isCreated()
                .returnResult(Void.class);


        String savedPostUri = result.getResponseHeaders().getLocation().toString();

        assertNotNull(savedPostUri);

        client
                .get()
                .uri(savedPostUri)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.title").isEqualTo(title)
                .jsonPath("$.content").isEqualTo(content)
                .jsonPath("$.createdDate").isNotEmpty();

        // added comment
        client
                .mutate().filter(basicAuthentication("user", "password")).build()
                .post()
                .uri(savedPostUri + "/comments")
                .bodyValue(Comment.builder().content("my comments").build())
                .exchange()
                .expectStatus().isCreated()
                .expectBody().isEmpty();

        client
                .get()
                .uri(savedPostUri + "/comments")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Comment.class).hasSize(1);

        client
                .mutate().filter(basicAuthentication("user", "password")).build()
                .put()
                .uri(savedPostUri)
                .bodyValue(Post.builder().title("updated title").content("updated content").build())
                .exchange()
                .expectStatus().isNoContent()
                .expectBody().isEmpty();


        client
                .mutate().filter(basicAuthentication("user", "password")).build()
                .delete()
                .uri(savedPostUri)
                .exchange()
                .expectStatus().isForbidden();

        client
                .mutate().filter(basicAuthentication("admin", "password")).build()
                .delete()
                .uri(savedPostUri)
                .exchange()
                .expectStatus().isNoContent();

    }

}
