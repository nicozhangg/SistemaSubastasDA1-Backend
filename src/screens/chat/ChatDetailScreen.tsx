import React, { useEffect, useRef, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TextInput,
  Pressable,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import { Colors, FontSize, Fonts } from '../../constants';

export default function ChatDetailScreen() {
  const navigation = useNavigation<any>();
  const initial = [
    { id: 'm1', type: 'out', text: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Proin eros felis, tristique ultricies nibh a, luctus scelerisque nunc. Fusce cursus augue et metus facilisis mattis.', time: '7:20' },
    { id: 'm2', type: 'in', text: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Proin eros felis, tristique ultricies nibh a, luctus scelerisque nunc. Fusce cursus augue et metus facilisis mattis.', time: '7:20' },
  ];

  const [messages, setMessages] = useState(initial);
  const [inputText, setInputText] = useState('');

  const scrollRef = useRef<any>(null);

  useEffect(() => {
    // Scroll to bottom on mount
    setTimeout(() => scrollRef.current?.scrollToEnd({ animated: false }), 0);
  }, []);

  useEffect(() => {
    // Scroll to bottom whenever messages change
    scrollRef.current?.scrollToEnd({ animated: true });
  }, [messages]);

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Pressable style={styles.back} onPress={() => navigation.goBack()}>
          <Ionicons name="chevron-back" size={24} color={Colors.white} />
        </Pressable>
        <View style={styles.headerInfo}>
          <View style={styles.headerAvatar}><Text style={styles.headerAvatarInitial}>M</Text></View>
          <View>
            <Text style={styles.headerTitle}>Marvin McKinney</Text>
            <Text style={styles.headerSubtitle}>De: Reloj vintage</Text>
          </View>
        </View>
      </View>

      <ScrollView
        ref={scrollRef}
        style={styles.messages}
        contentContainerStyle={styles.messagesContent}
        onContentSizeChange={() => scrollRef.current?.scrollToEnd({ animated: true })}
      >
        {messages.map((m) => (
          <View key={m.id} style={[styles.messageRow, m.type === 'out' ? styles.messageOutRow : styles.messageInRow]}>
            {m.type === 'in' ? (
              <View style={styles.bubbleIn}>
                <Text style={styles.bubbleText}>{m.text}</Text>
                <Text style={styles.msgTime}>{m.time}</Text>
              </View>
            ) : (
              <View style={styles.bubbleOut}>
                <Text style={styles.bubbleTextOut}>{m.text}</Text>
                <Text style={styles.msgTimeOut}>{m.time}</Text>
              </View>
            )}
          </View>
        ))}
      </ScrollView>

      <View style={styles.inputBar}>
        <TextInput
          placeholder="Escribí tu mensaje..."
          placeholderTextColor="#9A9A9A"
          style={styles.input}
          value={inputText}
          onChangeText={setInputText}
        />
        <Pressable
          style={styles.sendBtn}
          onPress={() => {
            const text = inputText.trim();
            if (!text) return;
            const now = new Date();
            const h = now.getHours().toString().padStart(2, '0');
            const m = now.getMinutes().toString().padStart(2, '0');
            const time = `${h}:${m}`;
            const newMsg = { id: `m${Date.now()}`, type: 'out', text, time };
            setMessages((prev) => [...prev, newMsg]);
            setInputText('');
          }}
        >
          <Ionicons name="send" size={20} color={Colors.white} />
        </Pressable>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.primary,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 12,
    paddingHorizontal: 12,
    backgroundColor: Colors.auctionViolet,
  },
  back: {
    marginRight: 8,
  },
  headerInfo: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  headerAvatar: {
    width: 46,
    height: 46,
    borderRadius: 23,
    backgroundColor: Colors.white,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 12,
  },
  headerAvatarInitial: {
    color: Colors.auctionViolet,
    fontWeight: '700',
  },
  headerTitle: {
    color: Colors.white,
    fontFamily: Fonts.soraBold,
    fontSize: FontSize.lg,
  },
  headerSubtitle: {
    color: '#E6E6F0',
    fontSize: FontSize.sm,
  },
  messages: {
    flex: 1,
    backgroundColor: Colors.white,
  },
  messagesContent: {
    padding: 16,
    paddingBottom: 120,
  },
  messageRow: {
    marginBottom: 12,
  },
  messageInRow: {
    alignItems: 'flex-start',
  },
  messageOutRow: {
    alignItems: 'flex-end',
  },
  bubbleIn: {
    backgroundColor: '#F2F2F6',
    padding: 12,
    borderRadius: 12,
    maxWidth: '80%',
  },
  bubbleOut: {
    backgroundColor: '#F7B65A',
    padding: 12,
    borderRadius: 12,
    maxWidth: '80%',
  },
  bubbleText: {
    color: Colors.textPrimary,
    lineHeight: 20,
  },
  bubbleTextOut: {
    color: Colors.white,
    lineHeight: 20,
  },
  msgTime: {
    color: Colors.textSecondary,
    fontSize: FontSize.xs,
    marginTop: 6,
  },
  msgTimeOut: {
    color: Colors.white,
    fontSize: FontSize.xs,
    marginTop: 6,
    alignSelf: 'flex-end',
  },
  inputBar: {
    position: 'absolute',
    left: 12,
    right: 12,
    bottom: 16,
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#F2F2F4',
    borderRadius: 24,
    paddingHorizontal: 12,
    paddingVertical: 8,
  },
  input: {
    flex: 1,
    fontSize: FontSize.base,
    paddingVertical: 6,
    color: Colors.textPrimary,
  },
  sendBtn: {
    backgroundColor: Colors.primary,
    padding: 8,
    borderRadius: 20,
    marginLeft: 8,
  },
});
