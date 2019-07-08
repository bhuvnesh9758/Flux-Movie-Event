package com.reactive.reactivemovie;

import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Date;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.ServerResponse.*;

@SpringBootApplication
public class ReactiveMovieApplication {
    @Bean
    ApplicationRunner demo(MovieService movieService){
        return args -> {
            Flux.just("Silence","capacitor","Anime","Danger","fluxer",
                    "procastinator","Ghost","Flux Gordon","Back to the future")
                    .map(title-> new Movie(title))
                .flatMap(movie -> movieService.add(movie))
                            .thenMany(movieService.getAllMovies())
                    .subscribe(System.out::println);

            movieService.getAllMovies()
                    .flatMap(movie-> movieService.getEvents(movie.getId()).take(10))
                    .subscribe(System.out::println);
//            movieService.getEvents(movieId).take(10)
//                    .subscribe(System.out::println);
        };
    }
    @Bean
    RouterFunction<?> routerFunction(MovieService movieService){
        return RouterFunctions.route(GET("/movies"),
                req-> ok().body(movieService.getAllMovies(),Movie.class))
                .andRoute(GET("/movies/{id}"),
                        req-> ok().body(movieService.getMovie(req.pathVariable("id")),Movie.class))
                .andRoute(GET("/movies/{id}/events"),
                        req-> ok().contentType(MediaType.TEXT_EVENT_STREAM).body(movieService.getEvents(req.pathVariable("id")),MovieEvent.class));
    }
    public static void main(String[] args) {
        SpringApplication.run(ReactiveMovieApplication.class,args);
    }

}
@Repository
interface MovieRepository extends ReactiveMongoRepository<Movie,String> {
    Flux<Movie> findByTitle(String title);
}
@Service
class MovieService{
    private final MovieRepository movieRepository;

    MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }
    public Mono<Movie> add(Movie movie){
        return this.movieRepository.save(movie);
    }
    public Flux<Movie> getAllMovies(){
        return this.movieRepository.findAll();
    }
    public Mono<Movie> getMovie(String id){
        return this.movieRepository.findById(id);
    }
    public Flux<MovieEvent> getEvents(String movieId){
        return Flux.<MovieEvent>generate(sink->sink.next(new MovieEvent(movieId,new Date())))
                .delayElements(Duration.ofSeconds(1));
    }
}
@Document
@Data
@AllArgsConstructor
@NoArgsConstructor
class MovieEvent{
    private String movieId;
    private Date dateViewed;
}
@Document
@Data
@NoArgsConstructor
@RequiredArgsConstructor
class Movie{
    @Id
    private  String id;
    @NonNull
    private  String title;
}