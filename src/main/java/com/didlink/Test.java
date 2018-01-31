package com.didlink;

import com.didlink.db.UserLocationDAO;
import com.didlink.models.UserLocation;

import java.io.IOException;

public class Test {
    public static void main(String[] args) throws Exception {
        UserLocation userLocation = new UserLocation(111, "",33233, 222.111,333.332,21212332233333332l);

        UserLocationDAO userLocationDAO = new UserLocationDAO();

        userLocationDAO.saveLocation(userLocation);

    }

}
