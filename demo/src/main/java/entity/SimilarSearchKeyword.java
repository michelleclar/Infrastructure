package entity;

import jakarta.persistence.*;

@Entity
@Table(name = "similar_search_keyword")
public class SimilarSearchKeyword {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "similar_search_keyword_id_gen")
    @SequenceGenerator(
            name = "similar_search_keyword_id_gen",
            sequenceName = "similar_search_keyword_id_seq",
            allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "keyword", nullable = false, length = Integer.MAX_VALUE)
    private String keyword;

    @Column(name = "sim_hash", nullable = false)
    private Long simHash;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Long getSimHash() {
        return simHash;
    }

    public void setSimHash(Long simHash) {
        this.simHash = simHash;
    }
}
