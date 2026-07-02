import type { LucideIcon } from "lucide-react";
import {
  AlertTriangle,
  BarChart3,
  CalendarDays,
  CircleCheck,
  CircleDollarSign,
  Clock,
  Hand,
  Package,
  ShoppingCart,
  Truck,
  User,
} from "lucide-react";

/** Noms d’icônes alignés sur le backend (`icon` dans les KPI) et l’UI locale. */
const registry: Record<string, LucideIcon> = {
  Package,
  AlertTriangle,
  ShoppingCart,
  CircleDollarSign,
  CalendarDays,
  Hand,
  Clock,
  Truck,
  CircleCheck,
  User,
  BarChart3,
};

export function LucideByName({
  name,
  className,
  size = 18,
  strokeWidth = 2,
  style,
}: {
  name: string;
  className?: string;
  size?: number;
  strokeWidth?: number;
  style?: React.CSSProperties;
}) {
  const Icon = registry[name] ?? Package;
  return <Icon className={className} size={size} strokeWidth={strokeWidth} style={style} />;
}
