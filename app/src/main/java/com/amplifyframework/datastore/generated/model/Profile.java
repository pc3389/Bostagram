package com.amplifyframework.datastore.generated.model;

import com.amplifyframework.core.model.annotations.HasMany;

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

/** This is an auto generated class representing the Profile type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "Profiles")
public final class Profile implements Model {
  public static final QueryField ID = field("id");
  public static final QueryField USERNAME = field("username");
  public static final QueryField NICKNAME = field("nickname");
  public static final QueryField EMAIL_ADDRESS = field("emailAddress");
  public static final QueryField PROFILE_IMAGE = field("profileImage");
  public static final QueryField BACKGROUND_IMAGE = field("backgroundImage");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String", isRequired = true) String username;
  private final @ModelField(targetType="String", isRequired = true) String nickname;
  private final @ModelField(targetType="String", isRequired = true) String emailAddress;
  private final @ModelField(targetType="String") String profileImage;
  private final @ModelField(targetType="String") String backgroundImage;
  private final @ModelField(targetType="Post") @HasMany(associatedWith = "profile", type = Post.class) List<Post> posts = null;
  public String getId() {
      return id;
  }
  
  public String getUsername() {
      return username;
  }
  
  public String getNickname() {
      return nickname;
  }
  
  public String getEmailAddress() {
      return emailAddress;
  }
  
  public String getProfileImage() {
      return profileImage;
  }
  
  public String getBackgroundImage() {
      return backgroundImage;
  }
  
  public List<Post> getPosts() {
      return posts;
  }
  
  private Profile(String id, String username, String nickname, String emailAddress, String profileImage, String backgroundImage) {
    this.id = id;
    this.username = username;
    this.nickname = nickname;
    this.emailAddress = emailAddress;
    this.profileImage = profileImage;
    this.backgroundImage = backgroundImage;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      Profile profile = (Profile) obj;
      return ObjectsCompat.equals(getId(), profile.getId()) &&
              ObjectsCompat.equals(getUsername(), profile.getUsername()) &&
              ObjectsCompat.equals(getNickname(), profile.getNickname()) &&
              ObjectsCompat.equals(getEmailAddress(), profile.getEmailAddress()) &&
              ObjectsCompat.equals(getProfileImage(), profile.getProfileImage()) &&
              ObjectsCompat.equals(getBackgroundImage(), profile.getBackgroundImage());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getUsername())
      .append(getNickname())
      .append(getEmailAddress())
      .append(getProfileImage())
      .append(getBackgroundImage())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("Profile {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("username=" + String.valueOf(getUsername()) + ", ")
      .append("nickname=" + String.valueOf(getNickname()) + ", ")
      .append("emailAddress=" + String.valueOf(getEmailAddress()) + ", ")
      .append("profileImage=" + String.valueOf(getProfileImage()) + ", ")
      .append("backgroundImage=" + String.valueOf(getBackgroundImage()))
      .append("}")
      .toString();
  }
  
  public static UsernameStep builder() {
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
  public static Profile justId(String id) {
    try {
      UUID.fromString(id); // Check that ID is in the UUID format - if not an exception is thrown
    } catch (Exception exception) {
      throw new IllegalArgumentException(
              "Model IDs must be unique in the format of UUID. This method is for creating instances " +
              "of an existing object with only its ID field for sending as a mutation parameter. When " +
              "creating a new object, use the standard builder method and leave the ID field blank."
      );
    }
    return new Profile(
      id,
      null,
      null,
      null,
      null,
      null
    );
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(id,
      username,
      nickname,
      emailAddress,
      profileImage,
      backgroundImage);
  }
  public interface UsernameStep {
    NicknameStep username(String username);
  }
  

  public interface NicknameStep {
    EmailAddressStep nickname(String nickname);
  }
  

  public interface EmailAddressStep {
    BuildStep emailAddress(String emailAddress);
  }
  

  public interface BuildStep {
    Profile build();
    BuildStep id(String id) throws IllegalArgumentException;
    BuildStep profileImage(String profileImage);
    BuildStep backgroundImage(String backgroundImage);
  }
  

  public static class Builder implements UsernameStep, NicknameStep, EmailAddressStep, BuildStep {
    private String id;
    private String username;
    private String nickname;
    private String emailAddress;
    private String profileImage;
    private String backgroundImage;
    @Override
     public Profile build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new Profile(
          id,
          username,
          nickname,
          emailAddress,
          profileImage,
          backgroundImage);
    }
    
    @Override
     public NicknameStep username(String username) {
        Objects.requireNonNull(username);
        this.username = username;
        return this;
    }
    
    @Override
     public EmailAddressStep nickname(String nickname) {
        Objects.requireNonNull(nickname);
        this.nickname = nickname;
        return this;
    }
    
    @Override
     public BuildStep emailAddress(String emailAddress) {
        Objects.requireNonNull(emailAddress);
        this.emailAddress = emailAddress;
        return this;
    }
    
    @Override
     public BuildStep profileImage(String profileImage) {
        this.profileImage = profileImage;
        return this;
    }
    
    @Override
     public BuildStep backgroundImage(String backgroundImage) {
        this.backgroundImage = backgroundImage;
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
    private CopyOfBuilder(String id, String username, String nickname, String emailAddress, String profileImage, String backgroundImage) {
      super.id(id);
      super.username(username)
        .nickname(nickname)
        .emailAddress(emailAddress)
        .profileImage(profileImage)
        .backgroundImage(backgroundImage);
    }
    
    @Override
     public CopyOfBuilder username(String username) {
      return (CopyOfBuilder) super.username(username);
    }
    
    @Override
     public CopyOfBuilder nickname(String nickname) {
      return (CopyOfBuilder) super.nickname(nickname);
    }
    
    @Override
     public CopyOfBuilder emailAddress(String emailAddress) {
      return (CopyOfBuilder) super.emailAddress(emailAddress);
    }
    
    @Override
     public CopyOfBuilder profileImage(String profileImage) {
      return (CopyOfBuilder) super.profileImage(profileImage);
    }
    
    @Override
     public CopyOfBuilder backgroundImage(String backgroundImage) {
      return (CopyOfBuilder) super.backgroundImage(backgroundImage);
    }
  }
  
}
