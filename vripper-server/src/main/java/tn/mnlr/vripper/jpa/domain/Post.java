package tn.mnlr.vripper.jpa.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import tn.mnlr.vripper.jpa.domain.enums.Status;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class Post {

    @JsonIgnore
    private Long id;

    private Status status;

    private String postId;

    private String threadTitle;

    private String threadId;

    private String title;

    private String url;

    private int done;

    private int total;

    private Set<String> hosts;

    private String forum;

    @JsonIgnore
    private String securityToken;

    @JsonIgnore
    private String postFolderName;

    private boolean thanked;

    private Set<String> previews = Collections.emptySet();

    private Metadata metadata;

    public Post(String title, String url, String postId, String threadId, String threadTitle, String forum, String securityToken) {
        this.title = title;
        this.url = url;
        this.postId = postId;
        this.forum = forum;
        this.threadId = threadId;
        this.threadTitle = threadTitle;
        this.securityToken = securityToken;
        status = Status.STOPPED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Post post = (Post) o;
        return Objects.equals(postId, post.postId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(postId);
    }
}
