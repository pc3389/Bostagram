package com.amplifyframework.datastore.generated.model;

import com.amplifyframework.core.model.annotations.BelongsTo;
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

/** This is an auto generated class representing the Post type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "Posts")
@Index(name = "byProfile", fields = {"profileID"})
public final class Post implements Model {
  public static final QueryField ID = field("id");
  public static final QueryField TITLE = field("title");
  public static final QueryField STATUS = field("status");
  public static final QueryField DATE = field("date");
  public static final QueryField CONTENTS = field("contents");
  public static final QueryField IMAGE = field("image");
  public static final QueryField PROFILE = field("profileID");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String", isRequired = true) String title;
  private final @ModelField(targetType="PostStatus", isRequired = true) PostStatus status;
  private final @ModelField(targetType="String", isRequired = true) String date;
  private final @ModelField(targetType="String") String contents;
  private final @ModelField(targetType="String") String image;
  private final @ModelField(targetType="Profile") @BelongsTo(targetName = "profileID", type = Profile.class) Profile profile;
  private final @ModelField(targetType="Comment") @HasMany(associatedWith = "post", type = Comment.class) List<Comment> comments = null;
  private final @ModelField(targetType="Like") @HasMany(associatedWith = "post", type = Like.class) List<Like> likes = null;
  public String getId() {
      return id;
  }
  
  public String getTitle() {
      return title;
  }
  
  public PostStatus getStatus() {
      return status;
  }
  
  public String getDate() {
      return date;
  }
  
  public String getContents() {
      return contents;
  }
  
  public String getImage() {
      return image;
  }
  
  public Profile getProfile() {
      return profile;
  }
  
  public List<Comment> getComments() {
      return comments;
  }
  
  public List<Like> getLikes() {
      return likes;
  }
  
  private Post(String id, String title, PostStatus status, String date, String contents, String image, Profile profile) {
    this.id = id;
    this.title = title;
    this.status = status;
    this.date = date;
    this.contents = contents;
    this.image = image;
    this.profile = profile;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      Post post = (Post) obj;
      return ObjectsCompat.equals(getId(), post.getId()) &&
              ObjectsCompat.equals(getTitle(), post.getTitle()) &&
              ObjectsCompat.equals(getStatus(), post.getStatus()) &&
              ObjectsCompat.equals(getDate(), post.getDate()) &&
              ObjectsCompat.equals(getContents(), post.getContents()) &&
              ObjectsCompat.equals(getImage(), post.getImage()) &&
              ObjectsCompat.equals(getProfile(), post.getProfile());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getTitle())
      .append(getStatus())
      .append(getDate())
      .append(getContents())
      .append(getImage())
      .append(getProfile())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("Post {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("title=" + String.valueOf(getTitle()) + ", ")
      .append("status=" + String.valueOf(getStatus()) + ", ")
      .append("date=" + String.valueOf(getDate()) + ", ")
      .append("contents=" + String.valueOf(getContents()) + ", ")
      .append("image=" + String.valueOf(getImage()) + ", ")
      .append("profile=" + String.valueOf(getProfile()))
      .append("}")
      .toString();
  }
  
  public static TitleStep builder() {
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
  public static Post justId(String id) {
    try {
      UUID.fromString(id); // Check that ID is in the UUID format - if not an exception is thrown
    } catch (Exception exception) {
      throw new IllegalArgumentException(
              "Model IDs must be unique in the format of UUID. This method is for creating instances " +
              "of an existing object with only its ID field for sending as a mutation parameter. When " +
              "creating a new object, use the standard builder method and leave the ID field blank."
      );
    }
    return new Post(
      id,
      null,
      null,
      null,
      null,
      null,
      null
    );
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(id,
      title,
      status,
      date,
      contents,
      image,
      profile);
  }
  public interface TitleStep {
    StatusStep title(String title);
  }
  

  public interface StatusStep {
    DateStep status(PostStatus status);
  }
  

  public interface DateStep {
    BuildStep date(String date);
  }
  

  public interface BuildStep {
    Post build();
    BuildStep id(String id) throws IllegalArgumentException;
    BuildStep contents(String contents);
    BuildStep image(String image);
    BuildStep profile(Profile profile);
  }
  

  public static class Builder implements TitleStep, StatusStep, DateStep, BuildStep {
    private String id;
    private String title;
    private PostStatus status;
    private String date;
    private String contents;
    private String image;
    private Profile profile;
    @Override
     public Post build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new Post(
          id,
          title,
          status,
          date,
          contents,
          image,
          profile);
    }
    
    @Override
     public StatusStep title(String title) {
        Objects.requireNonNull(title);
        this.title = title;
        return this;
    }
    
    @Override
     public DateStep status(PostStatus status) {
        Objects.requireNonNull(status);
        this.status = status;
        return this;
    }
    
    @Override
     public BuildStep date(String date) {
        Objects.requireNonNull(date);
        this.date = date;
        return this;
    }
    
    @Override
     public BuildStep contents(String contents) {
        this.contents = contents;
        return this;
    }
    
    @Override
     public BuildStep image(String image) {
        this.image = image;
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
    private CopyOfBuilder(String id, String title, PostStatus status, String date, String contents, String image, Profile profile) {
      super.id(id);
      super.title(title)
        .status(status)
        .date(date)
        .contents(contents)
        .image(image)
        .profile(profile);
    }
    
    @Override
     public CopyOfBuilder title(String title) {
      return (CopyOfBuilder) super.title(title);
    }
    
    @Override
     public CopyOfBuilder status(PostStatus status) {
      return (CopyOfBuilder) super.status(status);
    }
    
    @Override
     public CopyOfBuilder date(String date) {
      return (CopyOfBuilder) super.date(date);
    }
    
    @Override
     public CopyOfBuilder contents(String contents) {
      return (CopyOfBuilder) super.contents(contents);
    }
    
    @Override
     public CopyOfBuilder image(String image) {
      return (CopyOfBuilder) super.image(image);
    }
    
    @Override
     public CopyOfBuilder profile(Profile profile) {
      return (CopyOfBuilder) super.profile(profile);
    }
  }
  
}
