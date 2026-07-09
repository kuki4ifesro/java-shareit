package ru.practicum.shareit.item;

import ru.practicum.shareit.item.dto.ItemCreateDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemUpdateDto;

import java.util.List;

public interface ItemService {
    ItemDto createItem(ItemCreateDto itemDto, Long ownerId);

    ItemDto updateItem(Long itemId, ItemUpdateDto itemDto, Long ownerId);

    ItemDto getItemById(Long itemId);

    List<ItemDto> getItemsByOwner(Long ownerId);

    List<ItemDto> searchItems(String text);
}
