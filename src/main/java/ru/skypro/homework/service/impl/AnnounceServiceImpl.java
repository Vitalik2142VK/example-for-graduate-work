package ru.skypro.homework.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.skypro.homework.component.UserAuth;
import ru.skypro.homework.dto.announce.AnnouncesDtoOut;
import ru.skypro.homework.dto.announce.CreateOrUpdateAd;
import ru.skypro.homework.dto.announce.AnnounceDtoIn;
import ru.skypro.homework.dto.announce.AnnounceDtoOut;
import ru.skypro.homework.entity.Announce;
import ru.skypro.homework.entity.User;
import ru.skypro.homework.exception.NotFoundAnnounceException;
import ru.skypro.homework.exception.NotFoundUserException;
import ru.skypro.homework.exception.UserNotAuthorAnnounceException;
import ru.skypro.homework.helper.WorkWithImage;
import ru.skypro.homework.mapping.AnnounceMapper;
import ru.skypro.homework.repository.AnnounceRepository;
import ru.skypro.homework.repository.CommentRepository;
import ru.skypro.homework.repository.UserRepository;
import ru.skypro.homework.service.AnnounceService;

import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.Optional;

@Service
public class AnnounceServiceImpl implements AnnounceService {
    private final AnnounceRepository announceRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final AnnounceMapper announceMapper;

    @Value("${announce.image}")
    private String imagePath;
    @Value("${announce.url}")
    private String adsUrl;

    public AnnounceServiceImpl(AnnounceRepository announceRepository, UserRepository userRepository, CommentRepository commentRepository, AnnounceMapper announceMapper) {
        this.announceRepository = announceRepository;
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
        this.announceMapper = announceMapper;
    }

    /**
     *
     * the method returns all announces
     */
    @Override
    public AnnouncesDtoOut getAll() {
        return announceMapper.announceListToAnnounceDtoOutList(announceRepository.findAll());
    }

    /**
     *
     * the method returns all user announces
     */
    @Override
    public AnnouncesDtoOut getAllOfUser(UserAuth userDetails) {
        var userPk = userDetails.getUser().orElseThrow(NotFoundUserException::new).getId();
        return announceMapper.announceListToAnnounceDtoOutList(announceRepository.findAllByAuthor_Id(userPk));
    }

    /**
     *
     * the method returns announce by id
     */
    @Override
    public AnnounceDtoIn get(Integer id) {
        var announce = announceRepository.findById(id).orElseThrow(NotFoundAnnounceException::new);
        var announceDtoIn = new AnnounceDtoIn();

        fillUsersFields(announceDtoIn, Optional.ofNullable(announce.getAuthor()).orElseThrow(NotFoundUserException::new));

        announceDtoIn.setDescription(announce.getDescription());
        announceDtoIn.setImage(announce.getImage());
        announceDtoIn.setPrice(announce.getPrice());
        announceDtoIn.setTitle(announce.getTitle());
        return announceDtoIn;
    }

    /**
     *
     * the method add announce
     */
    @Override
    public AnnounceDtoOut add(CreateOrUpdateAd properties,
                              MultipartFile image,
                              UserAuth userDetails) throws IOException {
        User user = userDetails.getUser().orElseThrow(NotFoundUserException::new);
        int numberAds = announceRepository.getNumberUserAds(user.getId()) + 1;
        String fileName = "Ads_" + numberAds + "_auth_" + user.getId() + "_lg_" + user.getEmail().hashCode();
        Announce announce = new Announce();
        announce.setAuthor(user);
        announce.setDescription(properties.getDescription());
        announce.setPrice(properties.getPrice());
        announce.setTitle(properties.getTitle());
        announce.setImage(WorkWithImage.saveAndGetStringImage(adsUrl, imagePath, fileName, image));
        //todo добавить лог о добавлении объявления
        return announceMapper.toDTO(announceRepository.save(announce));
    }

    /**
     *
     * the method delete announce
     */
    @Override
    @PreAuthorize("hasRole('ADMIN') or @announceServiceImpl.checkAuthor(principal, #announceId)")
    public void delete(Integer announceId) {
        var announce = announceRepository.findById(announceId).orElseThrow(NotFoundAnnounceException::new);
        var comments = commentRepository.findAllByAd_IdOrderByCreatedAtDesc(announceId);
        if (comments != null && !(comments.isEmpty())) {
            comments.forEach(commentRepository::delete);
        }
        //todo добавить лог об удалении объявления
        announceRepository.delete(announce);
    }

    /**
     *
     * the method update announce info
     */
    @Override
    @PreAuthorize("hasRole('ADMIN') or @announceServiceImpl.checkAuthor(principal, #announceId)")
    public AnnounceDtoOut updateInfo(Integer announceId, CreateOrUpdateAd property) {
        var announce = announceRepository.findById(announceId).orElseThrow(NotFoundAnnounceException::new);

        announce.setDescription(property.getDescription());
        announce.setTitle(announce.getTitle());
        announce.setPrice(property.getPrice());
        return announceMapper.toDTO(announceRepository.save(announce));
    }

    /**
     *
     * the method update announce image
     */
    @Override
    @PreAuthorize("hasRole('ADMIN') or @announceServiceImpl.checkAuthor(principal, #announceId)")
    public String updateImage(Integer announceId, MultipartFile image) throws IOException {
        var announce = announceRepository.findById(announceId).orElseThrow(NotFoundAnnounceException::new);

        announce.setImage(WorkWithImage.updateAndGetStringImage(adsUrl, imagePath, announce.getImage(), image));
        announceRepository.save(announce);
        return announce.getImage();
    }

    /**
     *
     * the method checks the user
     */
    private boolean checkAuthor(Principal principal, int announceId) {
        int idUser = userRepository.getIdUserByEmail(principal.getName()).orElseThrow(NotFoundUserException::new);
        return announceRepository.checkAuthorAnnounce(announceId, idUser).orElseThrow(UserNotAuthorAnnounceException::new);
    }

    /**
     *
     * the method fills some of the fields of the AnnounceDtoIn object
     */
    private void fillUsersFields(AnnounceDtoIn announceDtoIn, User user) {
        announceDtoIn.setPk(user.getId());
        announceDtoIn.setAuthorFirstName(user.getFirstName());
        announceDtoIn.setAuthorLastName(user.getLastName());
        announceDtoIn.setEmail(user.getEmail());
        announceDtoIn.setPhone(user.getPhone());
    }
}
