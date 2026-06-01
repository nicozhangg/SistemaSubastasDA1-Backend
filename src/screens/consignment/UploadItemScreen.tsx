import React, { useCallback, useMemo, useState } from 'react';
import { Modal, Pressable, StyleSheet, Text, View } from 'react-native';
import { useNavigation } from '@react-navigation/native';
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
import type { HomeStackParamList } from '../../types';

type Nav = StackNavigationProp<HomeStackParamList, 'UploadItem'>;

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

export default function UploadItemScreen() {
  const navigation = useNavigation<Nav>();

  const [name, setName] = useState('');
  const [category, setCategory] = useState<string | null>(null);
  const [description, setDescription] = useState('');
  const [condition, setCondition] = useState<string | null>(null);
  const [currency, setCurrency] = useState<string | null>(null);
  const [suggestedPrice, setSuggestedPrice] = useState('');
  const [photos, setPhotos] = useState(createEmptyPhotoSlots);
  const [submitAttempted, setSubmitAttempted] = useState(false);
  const [successVisible, setSuccessVisible] = useState(false);

  const resetForm = useCallback(() => {
    setName('');
    setCategory(null);
    setDescription('');
    setCondition(null);
    setCurrency(null);
    setSuggestedPrice('');
    setPhotos(createEmptyPhotoSlots());
    setSubmitAttempted(false);
  }, []);

  const goHome = useCallback(() => {
    navigation.popToTop();
  }, [navigation]);

  const handleConfirm = useCallback(() => {
    const photoCount = photos.filter(Boolean).length;
    const missingFields: string[] = [];

    setSubmitAttempted(true);

    if (!name.trim()) {
      missingFields.push('nombre del artículo');
    }
    if (!category) {
      missingFields.push('categoría');
    }
    if (!description.trim()) {
      missingFields.push('descripción');
    }
    if (!condition) {
      missingFields.push('estado del artículo');
    }
    if (!currency) {
      missingFields.push('moneda');
    }
    if (!suggestedPrice.trim()) {
      missingFields.push('precio base sugerido');
    }
    if (photoCount < MIN_CONSIGNMENT_PHOTOS) {
      missingFields.push(`al menos ${MIN_CONSIGNMENT_PHOTOS} fotos`);
    }

    if (missingFields.length > 0) {
      return;
    }

    setSuccessVisible(true);
  }, [
    goHome,
    name,
    category,
    description,
    condition,
    currency,
    suggestedPrice,
    photos,
    resetForm,
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

  const handleStartOver = useCallback(() => {
    setSuccessVisible(false);
    resetForm();
  }, [resetForm]);

  const handleGoHome = useCallback(() => {
    setSuccessVisible(false);
    goHome();
  }, [goHome]);

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

      <Modal transparent visible={successVisible} animationType="fade" onRequestClose={() => setSuccessVisible(false)}>
        <Pressable style={styles.modalBackdrop} onPress={() => setSuccessVisible(false)}>
          <Pressable style={styles.modalCard} onPress={() => {}}>
            <Text style={styles.modalTitle}>Artículo subastado</Text>
            <Text style={styles.modalMessage}>
              Tu artículo fue enviado correctamente y ya quedó listo para revisión.
            </Text>
            <View style={styles.modalActions}>
              <Pressable style={[styles.modalButton, styles.modalButtonSecondary]} onPress={handleGoHome}>
                <Text style={[styles.modalButtonText, styles.modalButtonTextSecondary]}>Volver al inicio</Text>
              </Pressable>
              <Pressable style={[styles.modalButton, styles.modalButtonPrimary]} onPress={handleStartOver}>
                <Text style={styles.modalButtonText}>Subastar otro articulo</Text>
              </Pressable>
            </View>
          </Pressable>
        </Pressable>
      </Modal>
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
  modalBackdrop: {
    flex: 1,
    backgroundColor: 'rgba(27, 32, 69, 0.45)',
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 24,
  },
  modalCard: {
    width: '100%',
    backgroundColor: Colors.white,
    borderRadius: 18,
    padding: 20,
    shadowColor: Colors.black,
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.18,
    shadowRadius: 14,
    elevation: 8,
  },
  modalTitle: {
    fontFamily: Fonts.title,
    fontSize: FontSize.xl,
    color: Colors.black,
    textAlign: 'center',
    marginBottom: 8,
  },
  modalMessage: {
    fontFamily: Fonts.body,
    fontSize: FontSize.base,
    color: Colors.cardTime,
    textAlign: 'center',
    lineHeight: 22,
    marginBottom: 20,
  },
  modalActions: {
    gap: 10,
  },
  modalButton: {
    minHeight: 48,
    borderRadius: 14,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 16,
  },
  modalButtonPrimary: {
    backgroundColor: Colors.accent,
  },
  modalButtonSecondary: {
    backgroundColor: Colors.surface,
  },
  modalButtonText: {
    fontFamily: Fonts.bodyBold,
    fontSize: FontSize.base,
    color: Colors.white,
  },
  modalButtonTextSecondary: {
    color: Colors.textPrimary,
  },
});
