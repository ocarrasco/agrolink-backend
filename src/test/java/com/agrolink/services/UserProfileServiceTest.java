package com.agrolink.services;

import com.agrolink.dto.request.UpdateUserProfileRequest;
import com.agrolink.dto.response.UserProfileResponse;
import com.agrolink.mappers.UserProfileMapper;
import com.agrolink.model.UserProfileModel;
import com.agrolink.model.WeeklyAvailability;
import com.agrolink.model.enums.TimeSlot;
import com.agrolink.model.enums.UserRole;
import com.agrolink.repositories.IUserProfileRepository;
import com.agrolink.security.LoggedUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

  private static final LoggedUser USER = new LoggedUser(1, UUID.randomUUID(), UserRole.SUPPLIER);

  @Mock
  private IUserProfileRepository userProfileRepository;

  @Spy
  private UserProfileMapper userProfileMapper = Mappers.getMapper(UserProfileMapper.class);

  @InjectMocks
  private UserProfileService userProfileService;

  @Test
  void getMine_returnsAnEmptyDefault_whenTheUserHasNoProfileYet() {
    when(userProfileRepository.findByUserId(1)).thenReturn(Optional.empty());

    UserProfileResponse response = userProfileService.getMine(USER);

    assertThat(response.delivery()).isFalse();
    assertThat(response.address()).isNull();
    assertThat(response.phone()).isNull();
    assertThat(response.contactName()).isNull();
    assertThat(response.availability()).isEqualTo(WeeklyAvailability.empty());
  }

  @Test
  void getMine_returnsTheMappedProfile_whenItExists() {
    UserProfileModel profile = profile(1, true, "Av. Siempre Viva 123", "+56911111111", "Ana");
    profile.setAvailability(new WeeklyAvailability(List.of(TimeSlot.AM), null, null, null, null, null, null));
    when(userProfileRepository.findByUserId(1)).thenReturn(Optional.of(profile));

    UserProfileResponse response = userProfileService.getMine(USER);

    assertThat(response.delivery()).isTrue();
    assertThat(response.address()).isEqualTo("Av. Siempre Viva 123");
    assertThat(response.phone()).isEqualTo("+56911111111");
    assertThat(response.contactName()).isEqualTo("Ana");
    assertThat(response.availability().monday()).containsExactly(TimeSlot.AM);
    assertThat(response.availability().tuesday()).isEmpty();
  }

  @Test
  void upsert_createsAFreshProfile_whenTheUserHasNoneYet() {
    when(userProfileRepository.findByUserId(1)).thenReturn(Optional.empty());
    when(userProfileRepository.saveAndFlush(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));
    UpdateUserProfileRequest request = new UpdateUserProfileRequest(true, "Direccion 1", "+56922222222", "Beto",
        new WeeklyAvailability(List.of(TimeSlot.AM, TimeSlot.PM), null, null, null, null, null, null));

    UserProfileResponse response = userProfileService.upsert(USER, request);

    ArgumentCaptor<UserProfileModel> captor = ArgumentCaptor.forClass(UserProfileModel.class);
    verify(userProfileRepository).saveAndFlush(captor.capture());
    assertThat(captor.getValue().getUserId()).isEqualTo(1);
    assertThat(captor.getValue().getAddress()).isEqualTo("Direccion 1");

    assertThat(response.delivery()).isTrue();
    assertThat(response.address()).isEqualTo("Direccion 1");
    assertThat(response.phone()).isEqualTo("+56922222222");
    assertThat(response.contactName()).isEqualTo("Beto");
    assertThat(response.availability().monday()).containsExactly(TimeSlot.AM, TimeSlot.PM);
    assertThat(response.availability().tuesday()).isEmpty();
  }

  @Test
  void upsert_overwritesAnExistingProfile() {
    UserProfileModel existing = profile(1, false, "Direccion Vieja", "+56900000000", "Viejo Nombre");
    when(userProfileRepository.findByUserId(1)).thenReturn(Optional.of(existing));
    when(userProfileRepository.saveAndFlush(existing)).thenReturn(existing);
    UpdateUserProfileRequest request = new UpdateUserProfileRequest(true, "Direccion Nueva", "+56911111111", "Nombre Nuevo", null);

    UserProfileResponse response = userProfileService.upsert(USER, request);

    assertThat(existing.getUserId()).isEqualTo(1);
    assertThat(response.delivery()).isTrue();
    assertThat(response.address()).isEqualTo("Direccion Nueva");
    assertThat(response.phone()).isEqualTo("+56911111111");
    assertThat(response.contactName()).isEqualTo("Nombre Nuevo");
  }

  @Test
  void upsert_convertsBlankOptionalFieldsToNull() {
    UserProfileModel existing = profile(1, true, "Direccion", "Telefono", "Nombre");
    when(userProfileRepository.findByUserId(1)).thenReturn(Optional.of(existing));
    when(userProfileRepository.saveAndFlush(existing)).thenReturn(existing);
    UpdateUserProfileRequest request = new UpdateUserProfileRequest(true, "   ", "", null, null);

    UserProfileResponse response = userProfileService.upsert(USER, request);

    assertThat(response.address()).isNull();
    assertThat(response.phone()).isNull();
    assertThat(response.contactName()).isNull();
  }

  @Test
  void upsert_trimsSurroundingWhitespaceFromOptionalFields() {
    UserProfileModel existing = profile(1, true, null, null, null);
    when(userProfileRepository.findByUserId(1)).thenReturn(Optional.of(existing));
    when(userProfileRepository.saveAndFlush(existing)).thenReturn(existing);
    UpdateUserProfileRequest request = new UpdateUserProfileRequest(true, "  Direccion  ", null, null, null);

    UserProfileResponse response = userProfileService.upsert(USER, request);

    assertThat(response.address()).isEqualTo("Direccion");
  }

  @Test
  void upsert_defaultsAvailabilityToEmpty_whenRequestOmitsIt() {
    UserProfileModel existing = profile(1, true, null, null, null);
    when(userProfileRepository.findByUserId(1)).thenReturn(Optional.of(existing));
    when(userProfileRepository.saveAndFlush(existing)).thenReturn(existing);
    UpdateUserProfileRequest request = new UpdateUserProfileRequest(true, null, null, null, null);

    UserProfileResponse response = userProfileService.upsert(USER, request);

    assertThat(response.availability()).isEqualTo(WeeklyAvailability.empty());
  }

  @Test
  void ensureProfilesFor_returnsZero_whenUserIdsIsNull() {
    assertThat(userProfileService.ensureProfilesFor(null)).isZero();
    verify(userProfileRepository, never()).saveAll(anyCollection());
  }

  @Test
  void ensureProfilesFor_returnsZero_whenUserIdsIsEmpty() {
    assertThat(userProfileService.ensureProfilesFor(List.of())).isZero();
    verify(userProfileRepository, never()).saveAll(anyCollection());
  }

  @Test
  void ensureProfilesFor_onlyCreatesProfilesForMissingUsers() {
    when(userProfileRepository.findByUserIdIn(List.of(1, 2, 3))).thenReturn(List.of(profile(2, false, null, null, null)));

    int created = userProfileService.ensureProfilesFor(List.of(1, 2, 3));

    assertThat(created).isEqualTo(2);
    ArgumentCaptor<List<UserProfileModel>> captor = ArgumentCaptor.forClass(List.class);
    verify(userProfileRepository).saveAll(captor.capture());
    assertThat(captor.getValue()).extracting(UserProfileModel::getUserId).containsExactlyInAnyOrder(1, 3);
  }

  @Test
  void ensureProfilesFor_dedupesRepeatedUserIds() {
    when(userProfileRepository.findByUserIdIn(List.of(1, 1, 1))).thenReturn(List.of());

    int created = userProfileService.ensureProfilesFor(List.of(1, 1, 1));

    assertThat(created).isEqualTo(1);
  }

  @Test
  void ensureProfilesFor_createsNothing_whenEveryUserAlreadyHasAProfile() {
    when(userProfileRepository.findByUserIdIn(List.of(1, 2))).thenReturn(List.of(profile(1, false, null, null, null), profile(2, false, null, null, null)));

    int created = userProfileService.ensureProfilesFor(List.of(1, 2));

    assertThat(created).isZero();
    ArgumentCaptor<List<UserProfileModel>> captor = ArgumentCaptor.forClass(List.class);
    verify(userProfileRepository).saveAll(captor.capture());
    assertThat(captor.getValue()).isEmpty();
  }

  private static UserProfileModel profile(Integer userId, boolean delivery, String address, String phone, String contactName) {
    UserProfileModel model = new UserProfileModel();
    model.setUserId(userId);
    model.setDelivery(delivery);
    model.setAddress(address);
    model.setPhone(phone);
    model.setContactName(contactName);
    return model;
  }
}
