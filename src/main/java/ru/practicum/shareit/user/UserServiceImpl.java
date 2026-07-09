package ru.practicum.shareit.user;

import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.user.dto.UserCreateDto;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.dto.UserUpdateDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    private final Map<Long, User> users = new HashMap<>();
    private Long idCounter = 1L;

    @Override
    public UserDto createUser(UserCreateDto userDto) {
        validateEmailUnique(userDto.getEmail());
        User user = UserMapper.toUser(userDto);
        user.setId(idCounter++);
        users.put(user.getId(), user);
        return UserMapper.toUserDto(user);
    }

    @Override
    public UserDto updateUser(Long userId, UserUpdateDto userDto) {
        User existingUser = users.get(userId);
        if (existingUser == null) {
            throw new NotFoundException("User not found with id: " + userId);
        }

        if (userDto.getName() != null && !userDto.getName().isBlank()) {
            existingUser.setName(userDto.getName());
        }

        if (userDto.getEmail() != null && !userDto.getEmail().isBlank()) {
            if (!existingUser.getEmail().equals(userDto.getEmail())) {
                validateEmailUnique(userDto.getEmail());
            }
            existingUser.setEmail(userDto.getEmail());
        } else if (userDto.getEmail() != null && userDto.getEmail().isBlank()) {
            throw new ValidationException("Email cannot be blank");
        }

        return UserMapper.toUserDto(existingUser);
    }

    @Override
    public UserDto getUserById(Long userId) {
        User user = users.get(userId);
        if (user == null) {
            throw new NotFoundException("User not found with id: " + userId);
        }
        return UserMapper.toUserDto(user);
    }

    @Override
    public List<UserDto> getAllUsers() {
        return new ArrayList<>(users.values().stream()
                .map(UserMapper::toUserDto)
                .toList());
    }

    @Override
    public void deleteUser(Long userId) {
        users.remove(userId);
    }

    private void validateEmailUnique(String email) {
        for (User user : users.values()) {
            if (user.getEmail().equals(email)) {
                throw new ConflictException("Email already exists: " + email);
            }
        }
    }
}
