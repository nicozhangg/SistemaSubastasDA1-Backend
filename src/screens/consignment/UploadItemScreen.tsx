import React, { useCallback, useMemo, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
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

const CURRENCY_OPTIONS = [
  { value: 'ars', label: 'Pesos argentinos' },
  { value: 'usd', label: 'Dólares' },
];

const COMMISSION_PERCENT = 8;

function formatPrice(amount: string, currency: string | null) {
  const prefix = currency === 'usd' ? 'US$' : '$';
  return `${prefix}${amount}`;
}

export default function UploadItemScreen() {
  const navigation = useNavigation<Nav>();
  const route = useRoute<UploadRoute>();
  const addSubmission = useMyAuctionsStore((s) => s.addSubmission);
  const returnTo = route.params?.returnTo ?? 'home';

  const [name, setName] = useState('');
  const [category, setCategory] = useState<string | null>(null);
  const [description, setDescription] = useState('');
  const [condition, setCondition] = useState<string | null>(null);
  const [currency, setCurrency] = useState<string | null>(null);
  const [suggestedPrice, setSuggestedPrice] = useState('');
  const [photos, setPhotos] = useState(createEmptyPhotoSlots);
  const [submitAttempted, setSubmitAttempted] = useState(false);

  const handleConfirm = useCallback(() => {
    const photoCount = photos.filter(Boolean).length;

    setSubmitAttempted(true);

    if (
      !name.trim() ||
      !category ||
      !description.trim() ||
      !condition ||
      !currency ||
      !suggestedPrice.trim() ||
      photoCount < MIN_CONSIGNMENT_PHOTOS
    ) {
      return;
    }

    addSubmission({
      title: name.trim(),
      imageUrl: '',
      currentPrice: formatPrice(suggestedPrice.trim(), currency),
      status: 'soon',
    });

    navigation.navigate('ItemUploaded', { returnTo });
  }, [
    navigation,
    name,
    category,
    description,
    condition,
    currency,
    suggestedPrice,
    photos,
    addSubmission,
    returnTo,
  ]);

  const handleSuggestedPriceChange = useCallback((text: string) => {
    setSuggestedPrice(text.replace(/\D/g, ''));
  }, []);

  const hasMissingName = !name.trim();
  const hasMissingCategory = !category;
  const hasMissingDescription = !description.trim();
  const hasMissingCondition = !condition;
  const hasMissingCurrency = !currency;
  const hasMissingPrice = !suggestedPrice.trim();
  const hasMissingPhotos = photos.filter(Boolean).length < MIN_CONSIGNMENT_PHOTOS;
  const showErrors = submitAttempted;

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
        {showErrors && hasMissingName ? (
          <Text style={styles.fieldError}>El nombre del artículo es obligatorio.</Text>
        ) : null}
        <FormSelect
          placeholder="Categoría"
          options={CATEGORY_OPTIONS}
          value={category}
          onValueChange={setCategory}
        />
        {showErrors && hasMissingCategory ? (
          <Text style={styles.fieldError}>Seleccioná una categoría.</Text>
        ) : null}
        <FormTextArea
          placeholder="Descripción"
          value={description}
          onChangeText={setDescription}
        />
        {showErrors && hasMissingDescription ? (
          <Text style={styles.fieldError}>La descripción es obligatoria.</Text>
        ) : null}
        <FormSelect
          placeholder="Estado"
          options={CONDITION_OPTIONS}
          value={condition}
          onValueChange={setCondition}
        />
        {showErrors && hasMissingCondition ? (
          <Text style={styles.fieldError}>Seleccioná el estado del artículo.</Text>
        ) : null}
        <FormSelect
          placeholder="Moneda"
          options={CURRENCY_OPTIONS}
          value={currency}
          onValueChange={setCurrency}
        />
        {showErrors && hasMissingCurrency ? (
          <Text style={styles.fieldError}>Seleccioná una moneda.</Text>
        ) : null}
        <ConsignmentFormField
          placeholder="Precio base sugerido"
          value={suggestedPrice}
          onChangeText={handleSuggestedPriceChange}
          keyboardType="numeric"
        />
        {showErrors && hasMissingPrice ? (
          <Text style={styles.fieldError}>Ingresá un precio base sugerido.</Text>
        ) : null}
        <Text style={styles.commission}>
          Se cobrará una comisión del {COMMISSION_PERCENT}% del valor final.
        </Text>
      </View>

      <View style={styles.photosCard}>
        <PhotoUploadGrid photos={photos} onChange={setPhotos} />
        {showErrors && hasMissingPhotos ? (
          <Text style={styles.fieldError}>
            Debés adjuntar al menos {MIN_CONSIGNMENT_PHOTOS} fotos.
          </Text>
        ) : null}
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
  fieldError: {
    fontFamily: Fonts.body,
    fontSize: FontSize.xs,
    color: '#FF3B30',
    marginTop: -6,
    marginBottom: 10,
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
