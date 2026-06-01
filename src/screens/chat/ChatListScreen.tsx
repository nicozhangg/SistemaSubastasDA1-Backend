import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  Pressable,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import { Colors, FontSize, Fonts } from '../../constants';

const MOCK_CHATS = [
  {
    id: '1',
    name: 'Marvin McKinney',
    subtitle: 'Reloj vintage',
    preview: 'Lorem ipsum dolor sit amet',
    time: '5m',
    unread: true,
  },
  {
    id: '2',
    name: 'Leslie Alexander',
    subtitle: 'Pelota Final 2010 usada en partido',
    preview: 'Lorem ipsum dolor sit amet',
    time: '5m',
    unread: false,
  },
];

function ChatItem({ item }: { item: any }) {
  const navigation = useNavigation<any>();
  const [pressed, setPressed] = useState(false);

  return (
    <Pressable
      onPress={() => navigation.navigate('ChatDetail', { conversationId: item.id })}
      onPressIn={() => setPressed(true)}
      onPressOut={() => setPressed(false)}
      style={[styles.chatItem, (pressed || item.unread) && styles.chatItemActive]}
    >
      <View style={[styles.avatar, (pressed || item.unread) && styles.avatarActive]}>
        <Text style={styles.avatarInitial}>{item.name.split(' ')[0][0]}</Text>
      </View>

      <View style={styles.chatContent}>
        <View style={styles.chatTopRow}>
          <Text style={[styles.chatName, (pressed || item.unread) && styles.chatNameActive]}>{item.name}</Text>
          <Text style={[styles.chatTime, (pressed || item.unread) && styles.chatTimeActive]}>{item.time}</Text>
        </View>
        <Text style={[styles.chatSubtitle, (pressed || item.unread) && styles.chatSubtitleActive]}>{item.subtitle}</Text>
        <Text style={[styles.chatPreview, (pressed || item.unread) && styles.chatPreviewActive]}>{item.preview}</Text>
      </View>
    </Pressable>
  );
}

export default function ChatListScreen() {
  return (
    <View style={styles.container}>
      <View style={styles.headerRow}>
        <Text style={styles.headerTitle}>Chats</Text>
      </View>

      <FlatList
        data={MOCK_CHATS}
        keyExtractor={(i) => i.id}
        renderItem={({ item }) => <ChatItem item={item} />}
        contentContainerStyle={styles.list}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.white,
  },
  headerRow: {
    paddingHorizontal: 20,
    paddingTop: 18,
    paddingBottom: 12,
    backgroundColor: Colors.auctionViolet,
  },
  headerTitle: {
    color: Colors.white,
    fontFamily: Fonts.soraBold,
    fontSize: FontSize.xxl,
  },
  list: {
    backgroundColor: Colors.white,
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    paddingVertical: 12,
    paddingHorizontal: 12,
    marginTop: 8,
  },
  chatItem: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    padding: 12,
    borderRadius: 12,
    marginBottom: 10,
    backgroundColor: Colors.white,
  },
  chatItemActive: {
    backgroundColor: Colors.auctionViolet,
  },
  avatar: {
    width: 54,
    height: 54,
    borderRadius: 27,
    backgroundColor: Colors.surface,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 12,
  },
  avatarActive: {
    backgroundColor: Colors.white,
  },
  avatarInitial: {
    color: Colors.textPrimary,
    fontWeight: '700',
  },
  chatContent: {
    flex: 1,
  },
  chatTopRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 6,
  },
  chatName: {
    fontFamily: Fonts.soraBold,
    color: Colors.textPrimary,
    fontSize: FontSize.base,
  },
  chatNameActive: {
    color: Colors.white,
  },
  chatTime: {
    color: Colors.textSecondary,
    fontSize: FontSize.sm,
  },
  chatTimeActive: {
    color: '#E6E6F0',
  },
  chatSubtitle: {
    color: Colors.textSecondary,
    fontSize: FontSize.sm,
    marginBottom: 4,
  },
  chatSubtitleActive: {
    color: '#E6E6F0',
  },
  chatPreview: {
    color: '#B1B1B5',
    fontSize: FontSize.md,
  },
  chatPreviewActive: {
    color: '#F0EFFF',
  },
});
