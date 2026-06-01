import React, { useCallback, useMemo, useState } from 'react';
import { Alert, Pressable, StyleSheet, Text, View } from 'react-native';
import { useNavigation, useRoute } from '@react-navigation/native';
import type { RouteProp } from '@react-navigation/native';
import type { StackNavigationProp } from '@react-navigation/stack';
import { Ionicons } from '@expo/vector-icons';
import { PrimaryButton } from '../../components/auth';
import {
  ConsignmentFormField,
  ConsignmentScreenShell,
  FormSelect,
  FormTextArea,
  MIN_CONSIGNMENT_PHOTOS,
  PhotoUploadGrid,
  createEmptyPhotoSlots,
} from '../../components/consignment';
import { Colors, Fonts, FontSize } from '../../constants';
import { useMyAuctionsStore } from '../../stores';
import type { HomeStackParamList, MyAuctionsStackParamList } from '../../types';

type UploadRoute = RouteProp<
  HomeStackParamList | MyAuctionsStackParamList,
  'UploadItem'
>;
type Nav = StackNavigationProp<
  HomeStackParamList | MyAuctionsStackParamList,
  'UploadItem'
>;

const CATEGORY_OPTIONS = [
  { value: 'collectibles', label: 'Coleccionables' },
  { value: 'electronics', label: 'Electrónica' },
  { value: 'fashion', label: 'Moda' },
  { value: 'home', label: 'Hogar' },
  { value: 'other', label: 'Otros' },
];

const CONDITION_OPTIONS = [
  { value: 'new', label: 'Nuevo' },
  { value: 'like_new', label: 'Como nuevo' },
  { value: 'used', label: 'Usado' },
  { value: 'for_parts', label: 'Para repuestos' },
];

const COMMISSION_PERCENT = 8;

export default function UploadItemScreen() {
  const navigation = useNavigation<Nav>();
  const route = useRoute<UploadRoute>();
  const addSubmission = useMyAuctionsStore((s) => s.addSubmission);
  const returnTo = route.params?.returnTo ?? 'home';

  const [name, setName] = useState('');
  const [category, setCategory] = useState<string | null>(null);
  const [description, setDescription] = useState('');
  const [condition, setCondition] = useState<string | null>(null);
  const [suggestedPrice, setSuggestedPrice] = useState('');
  const [photos, setPhotos] = useState(createEmptyPhotoSlots);

  const handleConfirm = useCallback(() => {
    const photoCount = photos.filter(Boolean).length;

    if (!name.trim()) {
      Alert.alert('Subasta', 'Ingresá el nombre del artículo.');
      return;
    }
    if (!category) {
      Alert.alert('Subasta', 'Seleccioná una categoría.');
      return;
    }
    if (!description.trim()) {
      Alert.alert('Subasta', 'Agregá una descripción.');
      return;
    }
    if (!condition) {
      Alert.alert('Subasta', 'Seleccioná el estado del artículo.');
      return;
    }
    if (!suggestedPrice.trim()) {
      Alert.alert('Subasta', 'Ingresá el precio base sugerido.');
      return;
    }
    if (photoCount < MIN_CONSIGNMENT_PHOTOS) {
      Alert.alert(
        'Fotos',
        `Debés adjuntar al menos ${MIN_CONSIGNMENT_PHOTOS} fotos del artículo.`
      );
      return;
    }

    addSubmission({
      title: name.trim(),
      imageUrl: '',
      currentPrice: `$${suggestedPrice.trim()}`,
      status: 'soon',
    });

    navigation.navigate('ItemUploaded', { returnTo });
  }, [
    navigation,
    name,
    category,
    description,
    condition,
    suggestedPrice,
    photos,
    addSubmission,
    returnTo,
  ]);

  const footer = useMemo(
    () => <PrimaryButton label="Confirmar" onPress={handleConfirm} />,
    [handleConfirm]
  );

  return (
    <ConsignmentScreenShell footer={footer}>
      <Pressable
        style={({ pressed }) => [styles.backBtn, pressed && styles.backPressed]}
        onPress={() => navigation.goBack()}
        hitSlop={8}
      >
        <Ionicons name="chevron-back" size={22} color={Colors.textPrimary} />
      </Pressable>

      <Text style={styles.title}>Subastá tu artículo</Text>

      <View style={styles.formCard}>
        <ConsignmentFormField
          placeholder="Nombre del artículo"
          value={name}
          onChangeText={setName}
        />
        <FormSelect
          placeholder="Categoría"
          options={CATEGORY_OPTIONS}
          value={category}
          onValueChange={setCategory}
        />
        <FormTextArea
          placeholder="Descripción"
          value={description}
          onChangeText={setDescription}
        />
        <FormSelect
          placeholder="Estado"
          options={CONDITION_OPTIONS}
          value={condition}
          onValueChange={setCondition}
        />
        <ConsignmentFormField
          placeholder="Precio base sugerido"
          value={suggestedPrice}
          onChangeText={setSuggestedPrice}
          keyboardType="numeric"
        />
        <Text style={styles.commission}>
          Se cobrará una comisión del {COMMISSION_PERCENT}% del valor final.
        </Text>
      </View>

      <View style={styles.photosCard}>
        <PhotoUploadGrid photos={photos} onChange={setPhotos} />
      </View>
    </ConsignmentScreenShell>
  );
}

const styles = StyleSheet.create({
  backBtn: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: Colors.white,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 12,
    shadowColor: Colors.black,
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.08,
    shadowRadius: 4,
    elevation: 2,
  },
  backPressed: {
    opacity: 0.88,
  },
  title: {
    fontFamily: Fonts.title,
    fontSize: FontSize.xxl,
    color: Colors.black,
    marginBottom: 16,
  },
  formCard: {
    backgroundColor: Colors.white,
    borderRadius: 12,
    padding: 16,
    paddingBottom: 4,
    marginBottom: 12,
    shadowColor: Colors.black,
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.08,
    shadowRadius: 6,
    elevation: 3,
  },
  commission: {
    fontFamily: Fonts.body,
    fontSize: FontSize.xs,
    color: Colors.cardTime,
    marginTop: -4,
    marginBottom: 8,
    lineHeight: 16,
  },
  photosCard: {
    backgroundColor: Colors.white,
    borderRadius: 12,
    padding: 16,
    shadowColor: Colors.black,
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.08,
    shadowRadius: 6,
    elevation: 3,
  },
});
