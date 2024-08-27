package com.backend.app.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.sql.Timestamp;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.backend.app.models.entities.Author;
import com.backend.app.models.entities.Book;
import com.backend.app.models.entities.Publisher;
import com.backend.app.models.requests.BookRequest;
import com.backend.app.repositories.AuthorRepository;
import com.backend.app.repositories.BookRepository;
import com.backend.app.repositories.PublisherRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.backend.app.utils.redis.RedisHashUtil;

@Service
public class BookService {

    private static final String REDIS_KEY_PREFIX_BOOKS = "books";
    private BookRepository bookRepository;
    private PublisherRepository publisherRepository;
    private AuthorRepository authorRepository;
    // private RedisHashUtil redisHashUtil;

    @Autowired
    public BookService(BookRepository bookRepository, 
            PublisherRepository publisherRepository, AuthorRepository authorRepository) {
        this.bookRepository = bookRepository;
        this.publisherRepository = publisherRepository;
        this.authorRepository = authorRepository;
        // this.redisHashUtil = new RedisHashUtil(redisTemplate);
    }

    @Cacheable("books")
    public List<Book> getAllBooks() {
        try {

            // JSONArray booksFromCache = redisHashUtil.get(REDIS_KEY_PREFIX_BOOKS + ":*");
            // if (booksFromCache != null && !booksFromCache.isEmpty()) {
            //     ObjectMapper objectMapper = new ObjectMapper();

            //     objectMapper.registerModule(new JavaTimeModule());
            //     return objectMapper.readValue(booksFromCache.toString(), new TypeReference<List<Book>>() {
            //     });

            // }

            List<Book> books = this.bookRepository.findAll();
            if (books.isEmpty()) {
                return List.of();
            }

            // redisHashUtil.put(REDIS_KEY_PREFIX_BOOKS, books, Long.valueOf(60), TimeUnit.MINUTES);
            return books;

        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    @Cacheable(value = "books", key = "#id")
    public Optional<Book> getBookById(String id) {
        try {

            // JSONArray bookFromCache = this.redisHashUtil.get(REDIS_KEY_PREFIX_BOOKS + ":" + id);
            // if (bookFromCache != null && !bookFromCache.isEmpty()) {
            //     ObjectMapper objectMapper = new ObjectMapper();
            //     objectMapper.registerModule(new JavaTimeModule());

            //     // Get single object from JSONArray
            //     JSONObject book = bookFromCache.getJSONObject(0);

            //     return Optional.of(objectMapper.readValue(book.toString(), Book.class));
            // }

            Optional<Book> book = this.bookRepository.getBookById(id);
            if (book.isEmpty()) {
                return Optional.empty();
            }

            // this.redisHashUtil.put(REDIS_KEY_PREFIX_BOOKS, book.get(), Long.valueOf(60), TimeUnit.MINUTES);
            return book;
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }

    }

    public Optional<Book> createBook(BookRequest bookRequest) {
        try {
            Timestamp now = this.getCurrentTimestamp();
            Book book = new Book();
            book.setId(UUID.randomUUID().toString());
            book.setIsbn(bookRequest.getIsbn());
            book.setTitle(bookRequest.getTitle());
            book.setGenre(bookRequest.getGenre());
            book.setCopiesAvailable(bookRequest.getCopiesAvailable());
            book.setPrice(bookRequest.getPrice());
            book.setPublicationYear(bookRequest.getPublicationYear());
            book.setCreatedDate(now);
            book.setCreatedBy("system");
            book.setUpdatedDate(now);
            book.setUpdatedBy("system");

            if (bookRequest.getPublisherId() != null && !bookRequest.getPublisherId().isEmpty()) {
                Optional<Publisher> publisher = this.publisherRepository.getPublisherById(bookRequest.getPublisherId());
                book.setPublisher(publisher.isEmpty() ? null : publisher.get());
            }

            if (bookRequest.getAuthorIds() != null && !bookRequest.getAuthorIds().isEmpty()) {
                List<Author> authors = this.authorRepository.getAuthors(bookRequest.getAuthorIds());
                book.setAuthors(authors.isEmpty() ? null : authors);
            }

            Book newBook = this.bookRepository.save(book);
            if (newBook == null) {
                return Optional.empty();
            }

            // this.redisHashUtil.put(REDIS_KEY_PREFIX_BOOKS, book, Long.valueOf(60), TimeUnit.MINUTES);
            return Optional.of(newBook);
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public Optional<Book> updateBook(BookRequest bookRequest) {
        try {

            Optional<Book> book = this.getBookById(bookRequest.getId());
            if (book.isEmpty()) {
                return Optional.empty();
            }
            Timestamp now = this.getCurrentTimestamp();
            Book existingBook = book.get();

            existingBook.setTitle(bookRequest.getTitle().isEmpty() ? existingBook.getTitle() : bookRequest.getTitle());
            existingBook.setGenre(bookRequest.getGenre().isEmpty() ? existingBook.getGenre() : bookRequest.getGenre());
            existingBook.setCopiesAvailable(bookRequest.getCopiesAvailable() == existingBook.getCopiesAvailable() ? existingBook.getCopiesAvailable() : bookRequest.getCopiesAvailable());
            existingBook.setPrice(bookRequest.getPrice() == existingBook.getPrice() ? existingBook.getPrice() : bookRequest.getPrice());
            existingBook.setPublicationYear(bookRequest.getPublicationYear() == existingBook.getPublicationYear() ? existingBook.getPublicationYear() : bookRequest.getPublicationYear());
            existingBook.setUpdatedDate(now);
            existingBook.setUpdatedBy("system");
            
            Optional<Publisher> publisher = Optional.empty();
            if (bookRequest.getPublisherId() != null && !bookRequest.getPublisherId().isEmpty()) {
                publisher = this.publisherRepository.getPublisherById(bookRequest.getPublisherId());
            }
            existingBook.setPublisher(publisher.isEmpty() ? existingBook.getPublisher() : publisher.get());

            List<Author> authors = new ArrayList<>();
            if (bookRequest.getAuthorIds() != null && !bookRequest.getAuthorIds().isEmpty()) {
                authors = this.authorRepository.getAuthors(bookRequest.getAuthorIds());
            }
            existingBook.setAuthors(authors.isEmpty() ? existingBook.getAuthors() : authors);

            Book updatedBook = this.bookRepository.save(existingBook);
            if (updatedBook == null) {
                return Optional.empty();
            }

            // this.redisHashUtil.put(REDIS_KEY_PREFIX_BOOKS, existingBook, Long.valueOf(60), TimeUnit.MINUTES);
            return Optional.of(existingBook);

        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public Boolean deleteBook(String id) {
        try {
            // this.redisHashUtil.delete(REDIS_KEY_PREFIX_BOOKS + ":" + id);

            this.bookRepository.deleteBookByIdWithAuthors(id);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private Timestamp getCurrentTimestamp() {
        LocalDateTime currentDateTime = LocalDateTime.now();
        String formattedDateTime = currentDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime parsedDateTime = LocalDateTime.parse(formattedDateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return Timestamp.valueOf(parsedDateTime);
    }
    
}
