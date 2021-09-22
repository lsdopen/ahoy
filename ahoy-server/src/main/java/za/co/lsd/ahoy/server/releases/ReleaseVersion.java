/*
 * Copyright  2021 LSD Information Technology (Pty) Ltd
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package za.co.lsd.ahoy.server.releases;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import za.co.lsd.ahoy.server.applications.ApplicationVersion;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@Entity
@Data
@NoArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(name = "release_version", columnNames = {"release_id", "version"}))
public class ReleaseVersion implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull
    private String version;

    @ManyToOne
    private Release release;

    @ManyToMany
    @JoinTable(name = "RELEASE_VERSION_APPLICATION_VERSIONS",
            joinColumns = @JoinColumn(name = "RELEASE_VERSION_ID"),
            inverseJoinColumns = @JoinColumn(name = "APPLICATION_VERSIONS_ID"))
    @JsonIgnore
    @OrderBy("id")
    private List<ApplicationVersion> applicationVersions;

    @OneToMany(mappedBy = "releaseVersion", cascade = CascadeType.REMOVE, orphanRemoval = true)
	@JsonIgnore
	private List<ReleaseHistory> releaseHistories;

    public ReleaseVersion(@NotNull String version, Release release, List<ApplicationVersion> applicationVersions) {
        this.version = version;
        this.release = release;
        this.applicationVersions = applicationVersions;
    }

    public ReleaseVersion(@NotNull Long id, @NotNull String version, Release release, List<ApplicationVersion> applicationVersions) {
        this.id = id;
        this.version = version;
        this.release = release;
        this.applicationVersions = applicationVersions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReleaseVersion that = (ReleaseVersion) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ReleaseVersion{" +
                "id=" + id +
                ", version='" + version + '\'' +
                '}';
    }
}
