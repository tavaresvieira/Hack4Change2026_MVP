export type Role = 'STAFF';
export type Urgency = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type ItemCategory = 'FOOD' | 'CLOTHING' | 'HYGIENE' | 'BEDDING' | 'OTHER';
export type DonationStatus = 'OFFERED' | 'ASSIGNED' | 'DELIVERED' | 'RECEIVED';
export type DonationType = 'DROP_OFF' | 'PICKUP_REQUEST';
export type TransferStatus = 'PENDING' | 'IN_TRANSIT' | 'COMPLETED';
export type AnnouncementType = 'EXPIRY' | 'SURPLUS' | 'URGENT';

export interface Organization {
  id: number;
  name: string;
  type: string;
  address: string;
  populationServed: string;
  contactEmail: string;
  contactPhone: string;
}

export interface Item {
  id: number;
  name: string;
  description: string;
  unit: string;
  expiryRelevant: boolean;
  category: ItemCategory;
}

export interface Need {
  id: number;
  organization: Organization;
  item: Item;
  quantityNeeded: number;
  urgency: Urgency;
  fulfilled: boolean;
  createdAt: string;
}

export interface Donation {
  id: number;
  donorName: string;
  donorEmail: string;
  donorPhone: string | null;
  item: Item;
  quantity: number;
  expiryDate: string | null;
  status: DonationStatus;
  donationType: DonationType;
  pickupAddress: string | null;
  pickupCity: string | null;
  createdAt: string;
}

export interface Transfer {
  id: number;
  donation: Donation;
  toOrganization: Organization;
  quantityAssigned: number;
  status: TransferStatus;
  createdAt: string;
}

export interface Inventory {
  id: number;
  organization: Organization;
  item: Item;
  quantity: number;
  expiryDate: string | null;
}

export interface Announcement {
  id: number;
  organization: Organization;
  item: Item;
  quantity: number;
  type: AnnouncementType;
  message: string;
  expiryDate: string | null;
  createdAt: string;
}

export interface AuthResponse {
  id: number;
  token: string;
  name: string;
  email: string;
  role: Role;
  organizationId: number;
  organizationName: string | null;
}
