package com.example.monitoringdemo;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.apache.coyote.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@SpringBootApplication
public class MonitoringDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(MonitoringDemoApplication.class, args);
    }


    @Configuration
    public class MetricsConfig {
        @Bean
        public MeterRegistryCustomizer<MeterRegistry> commonTags() {
            return r -> r.config().commonTags("application", "demoApplication");
        }
    }

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

}

@RestController
class BookControler {

    private static final Logger logger = LoggerFactory.getLogger(BookControler.class);

    private final SlowBookRepository slowBookRepository;

    public BookControler(SlowBookRepository slowBookRepository) {
        this.slowBookRepository = slowBookRepository;
    }

    @GetMapping("/books/{id}")
    public ResponseEntity<Book> getBook(@PathVariable String id) throws InterruptedException {
        logger.debug("Request for books with id: {}", id);
        Optional<Book> book = this.slowBookRepository.findById(id);
        return book
                .map(b -> ResponseEntity.ok(new Book(id, UUID.randomUUID().toString())))
                .orElseGet(() -> {
                    logger.warn("Book with id: {} not found", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @PostMapping("/books")
	public Book saveBook(@RequestBody String bookName) {
    	return this.slowBookRepository.save(bookName);
	}

    @ExceptionHandler(value = UglyDbException.class)
	ResponseEntity<Void> handleUglyDbException(UglyDbException exception) {
    	logger.error("Db has failed us!", exception);
    	return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
	}


}

@Component
class SlowBookRepository {
    private static final Logger logger = LoggerFactory.getLogger(SlowBookRepository.class);

    private final Map<String, Book> db = new ConcurrentHashMap<>();

    public SlowBookRepository(MeterRegistry registry) {
        db.put("1", new Book("1", "Dune"));
        db.put("2", new Book("2", "In The Mountains Of Madness"));
        db.put("3", new Book("3", "Lord Of The Rings"));
        registry.gaugeMapSize("db.size", Tags.empty(), this.db);
    }

	@Counted("slowBookRepository.findById.counted")
    @Timed("slowBookRepository.findById.timed")
    public Optional<Book> findById(String id) throws InterruptedException {
        int randomNum = ThreadLocalRandom.current().nextInt(0, 6);
        if (randomNum == 4) {
            logger.debug("Sleeping for 500ms!");
            Thread.sleep(500);
        } else if (randomNum == 5) {
            throw new UglyDbException();
        }

        return Optional.ofNullable(db.get(id));
    }

    @Timed("slowBookRepository.save.timed")
	@Counted("slowBookRepository.timed.counted")
    public Book save(String bookName) {
    	Book book = new Book(UUID.randomUUID().toString(), bookName);
    	db.put(book.getId(), book);
    	return book;
	}
}

class UglyDbException extends RuntimeException {
    public UglyDbException() {
        super("One Ugly Db Exception!");
    }
}

class Book {
    private final String id;
    private final String name;

    public Book(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}