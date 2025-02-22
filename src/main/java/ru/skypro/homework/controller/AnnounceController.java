package ru.skypro.homework.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.skypro.homework.component.UserAuth;
import ru.skypro.homework.dto.announce.CreateOrUpdateAd;
import ru.skypro.homework.entity.Announce;
import ru.skypro.homework.exception.NotFoundAnnounceException;
import ru.skypro.homework.exception.NotFoundUserException;
import ru.skypro.homework.exception.UserNotAuthorAnnounceException;
import ru.skypro.homework.service.AnnounceService;

import java.io.IOException;

@CrossOrigin(value = "http://localhost:3000")
@RestController
@RequestMapping("/${announce.url}")
@Tag(name = "Объявления")
public class AnnounceController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnnounceController.class);

    private final AnnounceService announceService;

    public AnnounceController(AnnounceService announceService) {
        this.announceService = announceService;
    }

    @Operation(summary = "Получение всех объявлений")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK",
                    content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Announce.class))
                    })
    })
    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.status(HttpStatus.OK).body(announceService.getAll());
    }

    @Operation(summary = "Получение объявлений авторизированного пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK",
                    content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Announce.class))
                    }),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = {@Content(schema = @Schema(hidden = true))
                    })
    })
    @GetMapping("me")
    public ResponseEntity<?> getAllOfUser(@AuthenticationPrincipal UserAuth userDetails) {
        try {
            return ResponseEntity.status(HttpStatus.OK).body(announceService.getAllOfUser(userDetails));
        } catch (NotFoundUserException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @Operation(summary = "Добавление объявления")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created",
                    content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Announce.class))
                    }),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = {@Content(schema = @Schema(hidden = true))
                    })
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> add(@RequestPart CreateOrUpdateAd properties,
                                 @RequestPart MultipartFile image,
                                 @AuthenticationPrincipal UserAuth userDetails) throws IOException {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(announceService.add(properties, image, userDetails));
        } catch (NotFoundUserException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @Operation(summary = "Получение информации об объявлении")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK",
                    content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Announce.class))
                    }),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = {@Content(schema = @Schema(hidden = true))
                    }),
            @ApiResponse(responseCode = "404", description = "Not found",
                    content = {@Content(schema = @Schema(hidden = true))
                    })
    })
    @GetMapping("{id}")
    public ResponseEntity<?> get(@PathVariable Integer id) {
        try {
            return ResponseEntity.status(HttpStatus.OK).body(announceService.get(id));
        } catch (NotFoundUserException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (NotFoundAnnounceException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @Operation(summary = "Удаление объявления")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "No Content",
                    content = {@Content(schema = @Schema(hidden = true))
                    }),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = {@Content(schema = @Schema(hidden = true))
                    }),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = {@Content(schema = @Schema(hidden = true))
                    }),
            @ApiResponse(responseCode = "404", description = "Not found",
                    content = {@Content(schema = @Schema(hidden = true))
                    })
    })
    @DeleteMapping("{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id,
                                    @AuthenticationPrincipal UserAuth userDetails) {
        try {
            announceService.delete(id);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (NotFoundUserException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (UserNotAuthorAnnounceException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (NotFoundAnnounceException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IOException e) {
            LOGGER.error("Error writing file to output stream. Exception: '" + e.getMessage() + "'", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Обновление информации об объявлении")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK",
                    content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Announce.class))
                    }),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = {@Content(schema = @Schema(hidden = true))
                    }),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = {@Content(schema = @Schema(hidden = true))
                    }),
            @ApiResponse(responseCode = "404", description = "Not found",
                    content = {@Content(schema = @Schema(hidden = true))
                    })
    })
    @PatchMapping("{id}")
    public ResponseEntity<?> updateInfo(@PathVariable Integer id,
                                        @RequestBody CreateOrUpdateAd property) {
        try {
            return ResponseEntity.status(HttpStatus.OK).body(announceService.updateInfo(id, property));
        } catch (NotFoundUserException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (NotFoundAnnounceException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (UserNotAuthorAnnounceException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @Operation(summary = "Обновление картинки объявления")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK",
                    content = {@Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                            schema = @Schema(implementation = Announce.class))
                    }),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = {@Content(schema = @Schema(hidden = true))
                    }),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = {@Content(schema = @Schema(hidden = true))
                    }),
            @ApiResponse(responseCode = "404", description = "Not found",
                    content = {@Content(schema = @Schema(hidden = true))
                    })
    })
    @PatchMapping(value = "{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateImage(@PathVariable Integer id,
                                         @RequestPart MultipartFile image) throws IOException {
        try {
            return ResponseEntity.ok(announceService.updateImage(id, image));
        } catch (NotFoundUserException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (UserNotAuthorAnnounceException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (NotFoundAnnounceException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/${announce.image}/{name_image}")
    public ResponseEntity<?> getImage(@PathVariable("name_image") String nameImage) {
        try {
            return ResponseEntity.ok(announceService.getImage(nameImage));
        } catch (IOException e) {
            LOGGER.error("Error writing file to output stream. Exception: '" + e.getMessage() + "'", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
