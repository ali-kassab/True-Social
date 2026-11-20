package com.example.Social_Media_RestApi_Project.block;

import com.example.Social_Media_RestApi_Project.block.entity.Block;
import com.example.Social_Media_RestApi_Project.block.repository.BlockRepository;
import com.example.Social_Media_RestApi_Project.block.service.BlockService;
import com.example.Social_Media_RestApi_Project.exception.BadRequestException;
import com.example.Social_Media_RestApi_Project.exception.ResourceAlreadyExistsException;
import com.example.Social_Media_RestApi_Project.exception.ResourceNotFoundException;
import com.example.Social_Media_RestApi_Project.follow.entity.Follow;
import com.example.Social_Media_RestApi_Project.follow.repository.FollowRepository;
import com.example.Social_Media_RestApi_Project.security.CurrentUserProvider;
import com.example.Social_Media_RestApi_Project.support.TestData;
import com.example.Social_Media_RestApi_Project.user.entity.User;
import com.example.Social_Media_RestApi_Project.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlockServiceTest {

    @Mock private BlockRepository blockRepository;
    @Mock private FollowRepository followRepository;
    @Mock private UserRepository userRepository;
    @Mock private CurrentUserProvider currentUserProvider;

    @InjectMocks private BlockService blockService;

    private final User me = TestData.user(1L);
    private final User target = TestData.user(2L);

    @BeforeEach
    void setUp() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
    }

    @Test
    void block_savesBlockAndRemovesFollowsBothWays() {
        Follow iFollowThem = new Follow(me, target);
        Follow theyFollowMe = new Follow(target, me);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(blockRepository.existsByBlockerIdAndBlockedId(1L, 2L)).thenReturn(false);
        when(userRepository.getReferenceById(1L)).thenReturn(me);
        when(followRepository.findByFollowerIdAndFollowingId(1L, 2L)).thenReturn(Optional.of(iFollowThem));
        when(followRepository.findByFollowerIdAndFollowingId(2L, 1L)).thenReturn(Optional.of(theyFollowMe));

        blockService.block(2L);

        ArgumentCaptor<Block> saved = ArgumentCaptor.forClass(Block.class);
        verify(blockRepository).save(saved.capture());
        assertThat(saved.getValue().getBlocker()).isSameAs(me);
        assertThat(saved.getValue().getBlocked()).isSameAs(target);
        verify(followRepository).delete(iFollowThem);
        verify(followRepository).delete(theyFollowMe);
    }

    @Test
    void block_yourself_isBadRequest() {
        assertThatThrownBy(() -> blockService.block(1L)).isInstanceOf(BadRequestException.class);
        verify(blockRepository, never()).save(any());
    }

    @Test
    void block_twice_isConflict() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(blockRepository.existsByBlockerIdAndBlockedId(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> blockService.block(2L)).isInstanceOf(ResourceAlreadyExistsException.class);
        verify(blockRepository, never()).save(any());
    }

    @Test
    void block_unknownUser_isNotFound() {
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> blockService.block(2L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void unblock_whenNotBlocked_isNotFound() {
        when(blockRepository.findByBlockerIdAndBlockedId(1L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> blockService.unblock(2L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void unblock_removesOwnBlock() {
        Block block = new Block(me, target);
        when(blockRepository.findByBlockerIdAndBlockedId(1L, 2L)).thenReturn(Optional.of(block));

        blockService.unblock(2L);

        verify(blockRepository).delete(block);
    }
}
