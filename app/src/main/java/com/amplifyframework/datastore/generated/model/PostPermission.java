package com.amplifyframework.datastore.generated.model;


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

/** This is an auto generated class representing the PostPermission type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "PostPermissions")
public final class PostPermission implements Model {
  public static final QueryField ID = field("id");
  public static final QueryField USERNAME = field("username");
  public static final QueryField PERMISSION = field("permission");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String", isRequired = true) String username;
  private final @ModelField(targetType="Boolean", isRequired = true) Boolean permission;
  public String getId() {
      return id;
  }
  
  public String getUsername() {
      return username;
  }
  
  public Boolean getPermission() {
      return permission;
  }
  
  private PostPermission(String id, String username, Boolean permission) {
    this.id = id;
    this.username = username;
    this.permission = permission;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      PostPermission postPermission = (PostPermission) obj;
      return ObjectsCompat.equals(getId(), postPermission.getId()) &&
              ObjectsCompat.equals(getUsername(), postPermission.getUsername()) &&
              ObjectsCompat.equals(getPermission(), postPermission.getPermission());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getUsername())
      .append(getPermission())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("PostPermission {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("username=" + String.valueOf(getUsername()) + ", ")
      .append("permission=" + String.valueOf(getPermission()))
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
  public static PostPermission justId(String id) {
    try {
      UUID.fromString(id); // Check that ID is in the UUID format - if not an exception is thrown
    } catch (Exception exception) {
      throw new IllegalArgumentException(
              "Model IDs must be unique in the format of UUID. This method is for creating instances " +
              "of an existing object with only its ID field for sending as a mutation parameter. When " +
              "creating a new object, use the standard builder method and leave the ID field blank."
      );
    }
    return new PostPermission(
      id,
      null,
      null
    );
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(id,
      username,
      permission);
  }
  public interface UsernameStep {
    PermissionStep username(String username);
  }
  

  public interface PermissionStep {
    BuildStep permission(Boolean permission);
  }
  

  public interface BuildStep {
    PostPermission build();
    BuildStep id(String id) throws IllegalArgumentException;
  }
  

  public static class Builder implements UsernameStep, PermissionStep, BuildStep {
    private String id;
    private String username;
    private Boolean permission;
    @Override
     public PostPermission build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new PostPermission(
          id,
          username,
          permission);
    }
    
    @Override
     public PermissionStep username(String username) {
        Objects.requireNonNull(username);
        this.username = username;
        return this;
    }
    
    @Override
     public BuildStep permission(Boolean permission) {
        Objects.requireNonNull(permission);
        this.permission = permission;
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
    private CopyOfBuilder(String id, String username, Boolean permission) {
      super.id(id);
      super.username(username)
        .permission(permission);
    }
    
    @Override
     public CopyOfBuilder username(String username) {
      return (CopyOfBuilder) super.username(username);
    }
    
    @Override
     public CopyOfBuilder permission(Boolean permission) {
      return (CopyOfBuilder) super.permission(permission);
    }
  }
  
}
