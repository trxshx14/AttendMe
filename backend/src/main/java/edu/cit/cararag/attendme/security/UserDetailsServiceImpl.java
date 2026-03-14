package edu.cit.cararag.attendme.security;

import edu.cit.cararag.attendme.entity.User;
import edu.cit.cararag.attendme.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + username));

        // ✅ Block login if account is deactivated
        if (user.getIsActive() != null && !user.getIsActive()) {
            throw new DisabledException("Account is deactivated. Please contact your administrator.");
        }

        return UserDetailsImpl.build(user);
    }
}