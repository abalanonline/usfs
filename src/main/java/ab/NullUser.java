/*
 * Copyright 2020 Aleksei Balan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ab;

import org.apache.ftpserver.ftplet.Authentication;
import org.apache.ftpserver.ftplet.AuthenticationFailedException;
import org.apache.ftpserver.ftplet.AuthorizationRequest;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.usermanager.UsernamePasswordAuthentication;
import org.apache.ftpserver.usermanager.impl.BaseUser;

public class NullUser extends BaseUser implements UserManager {

  public static final NullUser MANAGER;

  static {
    final BaseUser user = new BaseUser();
    user.setEnabled(true);
    user.setHomeDirectory(".");
    MANAGER = new NullUser(user, "anonymous");
  }

  public NullUser(User user, String username) {
    super(user);
    setName(username);
  }

  @Override
  public User getUserByName(String username) {
    return new NullUser(this, username);
  }

  @Override
  public String[] getAllUserNames() {
    return new String[0];
  }

  @Override
  public void delete(String username) {

  }

  @Override
  public void save(User user) {

  }

  @Override
  public boolean doesExist(String username) {
    return true;
  }

  @Override
  public User authenticate(Authentication authentication) throws AuthenticationFailedException {
    if (authentication instanceof UsernamePasswordAuthentication) {
      return getUserByName(((UsernamePasswordAuthentication) authentication).getUsername());
    }
    return this; // AnonymousAuthentication
  }

  @Override
  public String getAdminName() {
    return null;
  }

  @Override
  public boolean isAdmin(String username) {
    return false;
  }

  @Override
  public AuthorizationRequest authorize(AuthorizationRequest request) {
    return request;
  }

}
