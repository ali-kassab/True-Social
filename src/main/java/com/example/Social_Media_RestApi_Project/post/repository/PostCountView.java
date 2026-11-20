package com.example.Social_Media_RestApi_Project.post.repository;

/** Projection for "count of X per post" aggregate queries. */
public interface PostCountView {
    Long getPostId();

    Long getTotal();
}
