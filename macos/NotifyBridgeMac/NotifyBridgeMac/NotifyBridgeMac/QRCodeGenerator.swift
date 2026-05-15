//
//  QRCodeGenerator.swift
//  NotifyBridgeMac
//
//  Created by Mahmut Alperen Ünal on 13.05.2026.
//

import SwiftUI
import CoreImage
import CoreImage.CIFilterBuiltins

/// Generates QR codes used for secure Android-to-Mac pairing.
enum QRCodeGenerator {

    /// Creates a high-resolution QR image from the provided pairing payload.
    static func generate(from text: String) -> NSImage? {
        let context = CIContext()
        let filter = CIFilter.qrCodeGenerator()

        filter.message = Data(text.utf8)
        filter.correctionLevel = "M"

        guard let outputImage = filter.outputImage else {
            return nil
        }

        // Scale the QR image to avoid blurry rendering in SwiftUI/AppKit views.
        let scaledImage = outputImage.transformed(by: CGAffineTransform(scaleX: 10, y: 10))

        guard let cgImage = context.createCGImage(scaledImage, from: scaledImage.extent) else {
            return nil
        }

        return NSImage(cgImage: cgImage, size: NSSize(width: 240, height: 240))
    }
}
