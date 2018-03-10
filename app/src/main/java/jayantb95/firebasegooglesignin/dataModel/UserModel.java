package jayantb95.firebasegooglesignin.dataModel;

import java.util.HashMap;

/**
 * Created by jayantb95 on 10/03/18.
 */

public class UserModel {
    private String email;
    private String name;
    private String photoUrl;

    public UserModel() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
}