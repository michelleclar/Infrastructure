// file:
// /Users/carl/workspace/backend/Infrastructure/infrastructure-component/pulsar/src/test/java/org/carl/infrastructure/pulsar/model/TestUser.java
package org.carl.infrastructure.pulsar.model;

public class TestUser {
    private Long id;
    private String name;
    private String email;
    private Integer age;

    public TestUser() {}

    public TestUser(Long id, String name, String email, Integer age) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.age = age;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestUser testUser = (TestUser) o;

        if (id != null ? !id.equals(testUser.id) : testUser.id != null) return false;
        if (name != null ? !name.equals(testUser.name) : testUser.name != null) return false;
        if (email != null ? !email.equals(testUser.email) : testUser.email != null) return false;
        return age != null ? age.equals(testUser.age) : testUser.age == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (email != null ? email.hashCode() : 0);
        result = 31 * result + (age != null ? age.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TestUser{"
                + "id="
                + id
                + ", name='"
                + name
                + '\''
                + ", email='"
                + email
                + '\''
                + ", age="
                + age
                + '}';
    }
}
