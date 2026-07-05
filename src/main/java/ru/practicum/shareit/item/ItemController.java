package ru.practicum.shareit.item;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.exception.MissingHeaderException;
import ru.practicum.shareit.item.dto.ItemDto;

import java.util.List;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {
    
    private final ItemService itemService;
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemDto createItem(@Valid @RequestBody ItemDto itemDto,
                              @RequestHeader(value = "X-Sharer-User-Id", required = false) Long ownerId) {
        if (ownerId == null) {
            throw new MissingHeaderException("X-Sharer-User-Id header is required");
        }
        return itemService.createItem(itemDto, ownerId);
    }
    
    @PatchMapping("/{itemId}")
    public ItemDto updateItem(@PathVariable Long itemId,
                              @Valid @RequestBody ItemDto itemDto,
                              @RequestHeader(value = "X-Sharer-User-Id", required = false) Long ownerId) {
        if (ownerId == null) {
            throw new MissingHeaderException("X-Sharer-User-Id header is required");
        }
        return itemService.updateItem(itemId, itemDto, ownerId);
    }
    
    @GetMapping("/{itemId}")
    public ItemDto getItemById(@PathVariable Long itemId,
                               @RequestHeader(value = "X-Sharer-User-Id", required = false) Long userId) {
        if (userId == null) {
            throw new MissingHeaderException("X-Sharer-User-Id header is required");
        }
        return itemService.getItemById(itemId);
    }
    
    @GetMapping
    public List<ItemDto> getItemsByOwner(@RequestHeader(value = "X-Sharer-User-Id", required = false) Long ownerId) {
        if (ownerId == null) {
            throw new MissingHeaderException("X-Sharer-User-Id header is required");
        }
        return itemService.getItemsByOwner(ownerId);
    }
    
    @GetMapping("/search")
    public List<ItemDto> searchItems(@RequestParam String text,
                                     @RequestHeader(value = "X-Sharer-User-Id", required = false) Long userId) {
        if (userId == null) {
            throw new MissingHeaderException("X-Sharer-User-Id header is required");
        }
        return itemService.searchItems(text);
    }
}
