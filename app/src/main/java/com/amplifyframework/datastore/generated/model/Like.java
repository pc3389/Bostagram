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

/** This is an auto generated class representing the Like type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "Likes")
@Index(name = "byPost", fields = {"postID"})
public final class Like implements Model {
  public static final QueryField ID = field("id");
  public static final QueryField PROFILE_ID = field("profileID");
  public static final QueryField POST = field("postID");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="ID", isRequired = true) String profileID;
  private final @ModelField(targetType="Post") @BelongsTo(targetName = "postID", type = Post.class) Post post;
  public String getId() {
      return id;
  }
  
  public String getProfileId() {
      return profileID;
  }
  
  public Post getPost() {
      return post;
  }
  
  private Like(String id, String profileID, Post post) {
    this.id = id;
    this.profileID = profileID;
    this.post = post;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      Like like = (Like) obj;
      return ObjectsCompat.equals(getId(), like.getId()) &&
              ObjectsCompat.equals(getProfileId(), like.getProfileId()) &&
              ObjectsCompat.equals(getPost(), like.getPost());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getProfileId())
      .append(getPost())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("Like {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("profileID=" + String.valueOf(getProfileId()) + ", ")
      .append("post=" + String.valueOf(getPost()))
      .append("}")
      .toString();
  }
  
  public static ProfileIdStep builder() {
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
  public static Like justId(String id) {
    try {
      UUID.fromString(id); // Check that ID is in the UUID format - if not an exception is thrown
    } catch (Exception exception) {
      throw new IllegalArgumentException(
              "Model IDs must be unique in the format of UUID. This method is for creating instances " +
              "of an existing object with only its ID field for sending as a mutation parameter. When " +
              "creating a new object, use the standard builder method and leave the ID field blank."
      );
    }
    return new Like(
      id,
      null,
      null
    );
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(id,
      profileID,
      post);
  }
  public interface ProfileIdStep {
    BuildStep profileId(String profileId);
  }
  

  public interface BuildStep {
    Like build();
    BuildStep id(String id) throws IllegalArgumentException;
    BuildStep post(Post post);
  }
  

  public static class Builder implements ProfileIdStep, BuildStep {
    private String id;
    private String profileID;
    private Post post;
    @Override
     public Like build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new Like(
          id,
          profileID,
          post);
    }
    
    @Override
     public BuildStep profileId(String profileId) {
        Objects.requireNonNull(profileId);
        this.profileID = profileId;
        return this;
    }
    
    @Override
     public BuildStep post(Post post) {
        this.post = post;
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
    private CopyOfBuilder(String id, String profileId, Post post) {
      super.id(id);
      super.profileId(profileId)
        .post(post);
    }
    
    @Override
     public CopyOfBuilder profileId(String profileId) {
      return (CopyOfBuilder) super.profileId(profileId);
    }
    
    @Override
     public CopyOfBuilder post(Post post) {
      return (CopyOfBuilder) super.post(post);
    }
  }
  
}
