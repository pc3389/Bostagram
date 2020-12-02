package com.amplifyframework.datastore.generated.model;

import com.amplifyframework.core.model.annotations.BelongsTo;

import java.util.List;
import java.util.UUID;
import java.util.Objects;

import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.annotations.Index;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.annotations.ModelField;
import com.amplifyframework.core.model.query.predicate.QueryField;

import static com.amplifyframework.core.model.query.predicate.QueryField.field;

/** This is an auto generated class representing the ProfileImage type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "ProfileImages")
@Index(name = "byProfile", fields = {"profileID"})
public final class ProfileImage implements Model {
  public static final QueryField ID = field("id");
  public static final QueryField DATE = field("date");
  public static final QueryField NUMBER = field("number");
  public static final QueryField PROFILE_IMAGE_KEY = field("profileImageKey");
  public static final QueryField PROFILE = field("profileID");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String", isRequired = true) String date;
  private final @ModelField(targetType="Int", isRequired = true) Integer number;
  private final @ModelField(targetType="String", isRequired = true) String profileImageKey;
  private final @ModelField(targetType="Profile") @BelongsTo(targetName = "profileID", type = Profile.class) Profile profile;
  public String getId() {
      return id;
  }
  
  public String getDate() {
      return date;
  }
  
  public Integer getNumber() {
      return number;
  }
  
  public String getProfileImageKey() {
      return profileImageKey;
  }
  
  public Profile getProfile() {
      return profile;
  }
  
  private ProfileImage(String id, String date, Integer number, String profileImageKey, Profile profile) {
    this.id = id;
    this.date = date;
    this.number = number;
    this.profileImageKey = profileImageKey;
    this.profile = profile;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      ProfileImage profileImage = (ProfileImage) obj;
      return ObjectsCompat.equals(getId(), profileImage.getId()) &&
              ObjectsCompat.equals(getDate(), profileImage.getDate()) &&
              ObjectsCompat.equals(getNumber(), profileImage.getNumber()) &&
              ObjectsCompat.equals(getProfileImageKey(), profileImage.getProfileImageKey()) &&
              ObjectsCompat.equals(getProfile(), profileImage.getProfile());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getDate())
      .append(getNumber())
      .append(getProfileImageKey())
      .append(getProfile())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("ProfileImage {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("date=" + String.valueOf(getDate()) + ", ")
      .append("number=" + String.valueOf(getNumber()) + ", ")
      .append("profileImageKey=" + String.valueOf(getProfileImageKey()) + ", ")
      .append("profile=" + String.valueOf(getProfile()))
      .append("}")
      .toString();
  }
  
  public static DateStep builder() {
      return new Builder();
  }
  
  /** 
   * WARNING: This method should not be used to build an instance of this object for a CREATE mutation.
   * This is a convenience method to return an instance of the object with only its ID populated
   * to be used in the context of a parameter in a delete mutation or referencing a foreign key
   * in a relationship.
   * @param id the id of the existing item this instance will represent
   * @return an instance of this model with only ID populated
   * @throws IllegalArgumentException Checks that ID is in the proper format
   */
  public static ProfileImage justId(String id) {
    try {
      UUID.fromString(id); // Check that ID is in the UUID format - if not an exception is thrown
    } catch (Exception exception) {
      throw new IllegalArgumentException(
              "Model IDs must be unique in the format of UUID. This method is for creating instances " +
              "of an existing object with only its ID field for sending as a mutation parameter. When " +
              "creating a new object, use the standard builder method and leave the ID field blank."
      );
    }
    return new ProfileImage(
      id,
      null,
      null,
      null,
      null
    );
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(id,
      date,
      number,
      profileImageKey,
      profile);
  }
  public interface DateStep {
    NumberStep date(String date);
  }
  

  public interface NumberStep {
    ProfileImageKeyStep number(Integer number);
  }
  

  public interface ProfileImageKeyStep {
    BuildStep profileImageKey(String profileImageKey);
  }
  

  public interface BuildStep {
    ProfileImage build();
    BuildStep id(String id) throws IllegalArgumentException;
    BuildStep profile(Profile profile);
  }
  

  public static class Builder implements DateStep, NumberStep, ProfileImageKeyStep, BuildStep {
    private String id;
    private String date;
    private Integer number;
    private String profileImageKey;
    private Profile profile;
    @Override
     public ProfileImage build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new ProfileImage(
          id,
          date,
          number,
          profileImageKey,
          profile);
    }
    
    @Override
     public NumberStep date(String date) {
        Objects.requireNonNull(date);
        this.date = date;
        return this;
    }
    
    @Override
     public ProfileImageKeyStep number(Integer number) {
        Objects.requireNonNull(number);
        this.number = number;
        return this;
    }
    
    @Override
     public BuildStep profileImageKey(String profileImageKey) {
        Objects.requireNonNull(profileImageKey);
        this.profileImageKey = profileImageKey;
        return this;
    }
    
    @Override
     public BuildStep profile(Profile profile) {
        this.profile = profile;
        return this;
    }
    
    /** 
     * WARNING: Do not set ID when creating a new object. Leave this blank and one will be auto generated for you.
     * This should only be set when referring to an already existing object.
     * @param id id
     * @return Current Builder instance, for fluent method chaining
     * @throws IllegalArgumentException Checks that ID is in the proper format
     */
    public BuildStep id(String id) throws IllegalArgumentException {
        this.id = id;
        
        try {
            UUID.fromString(id); // Check that ID is in the UUID format - if not an exception is thrown
        } catch (Exception exception) {
          throw new IllegalArgumentException("Model IDs must be unique in the format of UUID.",
                    exception);
        }
        
        return this;
    }
  }
  

  public final class CopyOfBuilder extends Builder {
    private CopyOfBuilder(String id, String date, Integer number, String profileImageKey, Profile profile) {
      super.id(id);
      super.date(date)
        .number(number)
        .profileImageKey(profileImageKey)
        .profile(profile);
    }
    
    @Override
     public CopyOfBuilder date(String date) {
      return (CopyOfBuilder) super.date(date);
    }
    
    @Override
     public CopyOfBuilder number(Integer number) {
      return (CopyOfBuilder) super.number(number);
    }
    
    @Override
     public CopyOfBuilder profileImageKey(String profileImageKey) {
      return (CopyOfBuilder) super.profileImageKey(profileImageKey);
    }
    
    @Override
     public CopyOfBuilder profile(Profile profile) {
      return (CopyOfBuilder) super.profile(profile);
    }
  }
  
}
