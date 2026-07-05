package ru.practicum.shareit.item;

import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemUpdateDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.UserService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ItemServiceImpl implements ItemService {

    private final Map<Long, Item> items = new HashMap<>();
    private Long idCounter = 1L;
    private final UserService userService;

    public ItemServiceImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    public ItemDto createItem(ItemDto itemDto, Long ownerId) {
        try {
            userService.getUserById(ownerId);
        } catch (NotFoundException e) {
            throw new NotFoundException("User not found with id: " + ownerId);
        }
        Item item = ItemMapper.toItem(itemDto, ownerId);
        item.setId(idCounter++);
        items.put(item.getId(), item);
        return ItemMapper.toItemDto(item);
    }

    @Override
    public ItemDto updateItem(Long itemId, ItemUpdateDto itemDto, Long ownerId) {
        Item existingItem = items.get(itemId);
        if (existingItem == null) {
            throw new NotFoundException("Item not found with id: " + itemId);
        }

        if (!existingItem.getOwnerId().equals(ownerId)) {
            throw new NotFoundException("User is not the owner of this item");
        }

        if (itemDto.getName() != null && !itemDto.getName().isBlank()) {
            existingItem.setName(itemDto.getName());
        }

        if (itemDto.getDescription() != null && !itemDto.getDescription().isBlank()) {
            existingItem.setDescription(itemDto.getDescription());
        }

        if (itemDto.getAvailable() != null) {
            existingItem.setAvailable(itemDto.getAvailable());
        }

        return ItemMapper.toItemDto(existingItem);
    }

    @Override
    public ItemDto getItemById(Long itemId) {
        Item item = items.get(itemId);
        if (item == null) {
            throw new NotFoundException("Item not found with id: " + itemId);
        }
        return ItemMapper.toItemDto(item);
    }

    @Override
    public List<ItemDto> getItemsByOwner(Long ownerId) {
        return items.values().stream()
                .filter(item -> item.getOwnerId().equals(ownerId))
                .map(ItemMapper::toItemDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ItemDto> searchItems(String text) {
        if (text == null || text.isBlank()) {
            return new ArrayList<>();
        }

        String lowerCaseText = text.toLowerCase();
        return items.values().stream()
                .filter(item -> item.getAvailable())
                .filter(item ->
                    (item.getName() != null && item.getName().toLowerCase().contains(lowerCaseText)) ||
                    (item.getDescription() != null && item.getDescription().toLowerCase().contains(lowerCaseText))
                )
                .map(ItemMapper::toItemDto)
                .collect(Collectors.toList());
    }
}
