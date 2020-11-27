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

/** This is an auto generated class representing the Comment type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "Comments")
@Index(name = "byPost", fields = {"postID","content"})
public final class Comment implements Model {
  public static final QueryField ID = field("id");
  public static final QueryField PROFILE_ID = field("profileID");
  public static final QueryField DATE = field("date");
  public static final QueryField NAME = field("name");
  public static final QueryField POST = field("postID");
  public static final QueryField CONTENT = field("content");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="ID", isRequired = true) String profileID;
  private final @ModelField(targetType="String", isRequired = true) String date;
  private final @ModelField(targetType="String", isRequired = true) String name;
  private final @ModelField(targetType="Post") @BelongsTo(targetName = "postID", type = Post.class) Post post;
  private final @ModelField(targetType="String", isRequired = true) String content;
  public String getId() {
      return id;
  }
  
  public String getProfileId() {
      return profileID;
  }
  
  public String getDate() {
      return date;
  }
  
  public String getName() {
      return name;
  }
  
  public Post getPost() {
      return post;
  }
  
  public String getContent() {
      return content;
  }
  
  private Comment(String id, String profileID, String date, String name, Post post, String content) {
    this.id = id;
    this.profileID = profileID;
    this.date = date;
    this.name = name;
    this.post = post;
    this.content = content;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      Comment comment = (Comment) obj;
      return ObjectsCompat.equals(getId(), comment.getId()) &&
              ObjectsCompat.equals(getProfileId(), comment.getProfileId()) &&
              ObjectsCompat.equals(getDate(), comment.getDate()) &&
              ObjectsCompat.equals(getName(), comment.getName()) &&
              ObjectsCompat.equals(getPost(), comment.getPost()) &&
              ObjectsCompat.equals(getContent(), comment.getContent());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getProfileId())
      .append(getDate())
      .append(getName())
      .append(getPost())
      .append(getContent())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("Comment {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("profileID=" + String.valueOf(getProfileId()) + ", ")
      .append("date=" + String.valueOf(getDate()) + ", ")
      .append("name=" + String.valueOf(getName()) + ", ")
      .append("post=" + String.valueOf(getPost()) + ", ")
      .append("content=" + String.valueOf(getContent()))
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
  public static Comment justId(String id) {
    try {
      UUID.fromString(id); // Check that ID is in the UUID format - if not an exception is thrown
    } catch (Exception exception) {
      throw new IllegalArgumentException(
              "Model IDs must be unique in the format of UUID. This method is for creating instances " +
              "of an existing object with only its ID field for sending as a mutation parameter. When " +
              "creating a new object, use the standard builder method and leave the ID field blank."
      );
    }
    return new Comment(
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
      profileID,
      date,
      name,
      post,
      content);
  }
  public interface ProfileIdStep {
    DateStep profileId(String profileId);
  }
  

  public interface DateStep {
    NameStep date(String date);
  }
  

  public interface NameStep {
    ContentStep name(String name);
  }
  

  public interface ContentStep {
    BuildStep content(String content);
  }
  

  public interface BuildStep {
    Comment build();
    BuildStep id(String id) throws IllegalArgumentException;
    BuildStep post(Post post);
  }
  

  public static class Builder implements ProfileIdStep, DateStep, NameStep, ContentStep, BuildStep {
    private String id;
    private String profileID;
    private String date;
    private String name;
    private String content;
    private Post post;
    @Override
     public Comment build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new Comment(
          id,
          profileID,
          date,
          name,
          post,
          content);
    }
    
    @Override
     public DateStep profileId(String profileId) {
        Objects.requireNonNull(profileId);
        this.profileID = profileId;
        return this;
    }
    
    @Override
     public NameStep date(String date) {
        Objects.requireNonNull(date);
        this.date = date;
        return this;
    }
    
    @Override
     public ContentStep name(String name) {
        Objects.requireNonNull(name);
        this.name = name;
        return this;
    }
    
    @Override
     public BuildStep content(String content) {
        Objects.requireNonNull(content);
        this.content = content;
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
    private CopyOfBuilder(String id, String profileId, String date, String name, Post post, String content) {
      super.id(id);
      super.profileId(profileId)
        .date(date)
        .name(name)
        .content(content)
        .post(post);
    }
    
    @Override
     public CopyOfBuilder profileId(String profileId) {
      return (CopyOfBuilder) super.profileId(profileId);
    }
    
    @Override
     public CopyOfBuilder date(String date) {
      return (CopyOfBuilder) super.date(date);
    }
    
    @Override
     public CopyOfBuilder name(String name) {
      return (CopyOfBuilder) super.name(name);
    }
    
    @Override
     public CopyOfBuilder content(String content) {
      return (CopyOfBuilder) super.content(content);
    }
    
    @Override
     public CopyOfBuilder post(Post post) {
      return (CopyOfBuilder) super.post(post);
    }
  }
  
}
